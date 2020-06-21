package not.ogame.bots.ghostbuster.processors

import cats.implicits._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.ghostbuster.executor._
import not.ogame.bots.ghostbuster.ogame.OgameAction
import not.ogame.bots.ghostbuster.{ExpeditionConfig, FLogger, FleetShip}

class ExpeditionProcessor(expeditionConfig: ExpeditionConfig, ogameDriver: OgameDriver[OgameAction])(
    implicit executor: OgameActionExecutor[Task],
    clock: LocalClock
) extends FLogger {
  def run(): Task[Unit] = {
    if (expeditionConfig.isOn) {
      ogameDriver
        .readPlanets()
        .execute()
        .map(planets => planets.filter(p => expeditionConfig.eligiblePlanets.contains(p.id)))
        .flatMap(lookForFleet)
        .onError(e => Logger[Task].error(e)(s"restarting expedition processor ${e.getMessage}"))
        .onErrorRestartIf(_ => true)
    } else {
      Task.never
    }
  }

  private def lookForFleet(planets: List[PlayerPlanet]): Task[Unit] = {
    for {
      myFleets <- ogameDriver.readMyFleets().execute()
      expeditions = myFleets.fleets.filter(_.fleetMissionType == FleetMissionType.Expedition)
      _ <- if (expeditions.size < expeditionConfig.maxNumberOfExpeditions) {
        Logger[Task].info(
          s"Only ${expeditions.size}/${expeditionConfig.maxNumberOfExpeditions} expeditions are in the air"
        ) >>
          lookForFleetOnPlanets(planets, myFleets.fleets) >> lookForFleet(planets)
      } else {
        val collectingDebris = collectDebris(planets, expeditions)
        val min = expeditions.map(_.arrivalTime).min
        for {
          _ <- collectingDebris.execute()
          _ <- Logger[Task].info(s"All expeditions are in the air, waiting for first to reach its target - $min")
          _ <- executor.waitTo(min)
          _ <- lookForFleet(planets)
        } yield ()
      }
    } yield ()
  }

  private def collectDebris(planets: List[PlayerPlanet], expeditions: List[MyFleet]) = {
    val shouldCollectDebris = expeditionConfig.collectingOn && (expeditions.filter(_.isReturning) match {
      case l if l.nonEmpty =>
        !expeditionConfig.ships.forall { case FleetShip(shipType, amount) => amount <= l.maxBy(_.arrivalTime).ships(shipType) }
      case Nil => false
    })
    if (shouldCollectDebris) {
      val debrisCollectingPlanet = planets.filter(_.id == expeditionConfig.collectingPlanet).head
      ogameDriver
        .readFleetPage(debrisCollectingPlanet.id)
        .flatMap { pf =>
          if (pf.ships(ShipType.Explorer) >= 300) {
            Logger[OgameAction].info("Will send fleet to collect debris...") >>
              ogameDriver
                .sendFleet(
                  SendFleetRequest(
                    debrisCollectingPlanet,
                    SendFleetRequestShips.Ships(Map(ShipType.Explorer -> 300)),
                    expeditionConfig.target.copy(coordinatesType = CoordinatesType.Debris),
                    FleetMissionType.Recycle,
                    FleetResources.Given(Resources.Zero)
                  )
                )
          } else {
            Logger[OgameAction].warn("There is not enough ships to collect debris")
          }
        }
    } else {
      Logger[OgameAction].info("Returning fleet is ok, no need to collect debris")
    }
  }

  private def sendExpedition(fromPlanet: PlayerPlanet) = {
    (for {
      _ <- Logger[OgameAction].info("Sending fleet...")
      _ <- ogameDriver.sendFleet(
        SendFleetRequest(
          fromPlanet,
          SendFleetRequestShips.Ships(expeditionConfig.ships.map(s => s.shipType -> s.amount).toMap),
          expeditionConfig.target,
          FleetMissionType.Expedition,
          FleetResources.Given(Resources.Zero)
        )
      )
      _ <- Logger[OgameAction].info("Fleet sent")
    } yield ())
      .recoverWith {
        case AvailableDeuterExceeded(requiredAmount) =>
          Logger[OgameAction].info(s"There was not enough deuter($requiredAmount), expedition won't be send this time")
      }
  }

  private def lookForFleetOnPlanets(planets: List[PlayerPlanet], allFleets: List[MyFleet]) = {
    Stream
      .emits(planets)
      .evalMap(p => ogameDriver.readFleetPage(p.id).map(p -> _))
      .collectFirst { case (planet, fleet) if isExpeditionFleet(fleet.ships) => planet }
      .evalMap(sendExpedition)
      .compile
      .last
      .execute()
      .flatMap {
        case Some(_) => ().pure[Task]
        case None    => waitToEarliestFleet(allFleets)
      }
  }

  private def waitToEarliestFleet(allFleets: List[MyFleet]) = {
    val tenMinutesFromNow = clock.now().plusMinutes(10)
    val minAnyFleetArrivalTime = minOr(allFleets.map(_.arrivalTime))(tenMinutesFromNow)
    val waitTo = List(minAnyFleetArrivalTime, tenMinutesFromNow).min
    Logger[Task].info(s"Could find expedition fleet on any planet. Waiting til $waitTo....") >> executor.waitTo(waitTo)
  }

  private def minOr[R: Ordering](l: List[R])(or: => R): R = {
    l match {
      case l if l.nonEmpty => l.min
      case _               => or
    }
  }

  private def isExpeditionFleet(fleet: Map[ShipType, Int]): Boolean = {
    expeditionConfig.ships.forall(ship => ship.amount <= fleet(ship.shipType))
  }
}
