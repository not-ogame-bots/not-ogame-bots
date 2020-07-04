package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import cats.implicits._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.ghostbuster.executor._
import not.ogame.bots.ghostbuster.ogame.OgameAction
import not.ogame.bots.ghostbuster.{FLogger, FsConfig}

import scala.concurrent.duration._

class FlyAndBuildProcessor(ogameDriver: OgameDriver[OgameAction], fsConfig: FsConfig, builder: Builder)(
    implicit executor: OgameActionExecutor[Task],
    clock: LocalClock
) extends FLogger {
  def run(): Task[Unit] = {
    if (fsConfig.isOn) {
      ogameDriver
        .readPlanets()
        .map(_.filter(p => fsConfig.eligiblePlanets.contains(p.id)))
        .execute()
        .flatMap(pl => withRetry(lookOnPlanets(pl))("flyAndBuild"))
    } else {
      Task.never
    }
  }

  private def lookAtInTheAir(planets: List[PlayerPlanet]): Task[Unit] = {
    (for {
      fleets <- ogameDriver.readAllFleetsRedirect()
      possibleFsFleets = fleets.filter(f => isFsFleet(planets, f))
      waitingTime <- possibleFsFleets match {
        case l @ _ :: _ =>
          Logger[OgameAction]
            .info("Too many fleets in the air. Waiting for the first one to reach its target.")
            .as(l.map(_.arrivalTime).min)
        case Nil =>
          Logger[OgameAction]
            .warn(s"Couldn't find fs fleet either on planets or in the air. Waiting ${fsConfig.searchInterval}...")
            .as(clock.now().plus(fsConfig.searchInterval))
      }
    } yield waitingTime)
      .execute()
      .flatMap(waitingTime => executor.waitTo(waitingTime))
      .flatMap(_ => lookOnPlanets(planets))
  }

  private def isFsFleet(planets: List[PlayerPlanet], f: Fleet) = {
    val eligiblePlanets = fsConfig.eligiblePlanets.map(pId => planets.find(_.id == pId).get)
    f.fleetAttitude == FleetAttitude.Friendly && f.fleetMissionType == FleetMissionType.Deployment && eligiblePlanets
      .exists(p => p.coordinates == f.to) && eligiblePlanets.exists(p => p.coordinates == f.from)
  }

  private def lookOnPlanets(planets: List[PlayerPlanet]): Task[Unit] = {
    Stream
      .emits(planets)
      .evalMap(p => ogameDriver.readFleetPage(p.id).map(p -> _))
      .collectFirst { case (p, f) if isFsFleet(f.ships) => p }
      .compile
      .last
      .execute()
      .flatMap {
        case Some(planet) =>
          Logger[Task].info(s"Planet with fs fleet ${pprint.apply(planet)}") >>
            buildAndSend(planet, planets)
        case None =>
          Logger[Task].warn("Couldn't find fs fleet on any planet, looking in the air...") >>
            lookAtInTheAir(planets)
      }
  }

  private def isFsFleet(fleet: Map[ShipType, Int]): Boolean = {
    fsConfig.ships.forall(fsShip => fsShip.amount <= fleet(fsShip.shipType)) //TODO display unmatched pairs
  }

  private def buildAndSend(currentPlanet: PlayerPlanet, planets: List[PlayerPlanet]): Task[Unit] = {
    val targetPlanet = nextPlanet(currentPlanet, planets)
    for {
      _ <- buildAndContinue(currentPlanet, clock.now())
      _ <- sendFleet(from = currentPlanet, to = targetPlanet)
      _ <- lookAtInTheAir(planets)
    } yield ()
  }

  private def nextPlanet(currentPlanet: PlayerPlanet, planets: List[PlayerPlanet]) = {
    val idx = (planets.indexOf(currentPlanet) + 1) % planets.size
    planets(idx)
  }

  private def sendFleet(from: PlayerPlanet, to: PlayerPlanet): Task[Unit] = {
    for {
      _ <- Logger[Task].info("Sending fleet...")
      _ <- sendFleetImpl(from, to)
        .execute()
        .flatTap(_ => Logger[Task].info("Fleet sent"))
        .recoverWith {
          case AvailableDeuterExceeded(requiredAmount) =>
            ogameDriver
              .readSuppliesPage(from.id)
              .execute()
              .flatMap { suppliesPageData =>
                val missingDeuter = requiredAmount - suppliesPageData.currentResources.deuterium
                val timeToProduceInHours = missingDeuter.toDouble / suppliesPageData.currentProduction.deuterium
                val timeInSeconds = (timeToProduceInHours * 60 * 60).toInt
                val maxWaitingTime = Math.min(timeInSeconds, 60 * 10)
                Logger[Task].info(s"There was not enough deuter, fleet sending delayed by $maxWaitingTime seconds") >>
                  Task.sleep(maxWaitingTime seconds) >>
                  sendFleet(from, to)
              }
        }
    } yield ()
  }

  private def sendFleetImpl(from: PlayerPlanet, to: PlayerPlanet) = {
    for {
      resources <- if (fsConfig.takeResources) {
        new ResourceSelector[OgameAction](deuteriumSelector = Selector.decreaseBy(fsConfig.remainDeuterAmount))
          .selectResources(ogameDriver, from)
      } else {
        Resources.Zero.pure[OgameAction]
      }
      _ <- ogameDriver
        .sendFleet(
          SendFleetRequest(
            from,
            if (fsConfig.gatherShips) {
              SendFleetRequestShips.AllShips
            } else {
              SendFleetRequestShips.Ships(fsConfig.ships.map(s => s.shipType -> s.amount).toMap)
            },
            to.coordinates,
            FleetMissionType.Deployment,
            FleetResources.Given(resources),
            fsConfig.fleetSpeed
          )
        )
    } yield ()
  }

  private def buildAndContinue(planet: PlayerPlanet, startedBuildingAt: ZonedDateTime): Task[Unit] = {
    if (fsConfig.builder) {
      builder
        .buildNextThingFromWishList(planet)
        .execute()
        .flatMap {
          case BuilderResult.Building(finishTime)
              if timeDiff(clock.now(), finishTime) < fsConfig.maxBuildingTime && timeDiff(startedBuildingAt, clock.now()) < fsConfig.maxWaitTime =>
            Logger[Task].info(s"Decided to wait for building to finish til $finishTime") >>
              executor.waitTo(finishTime) >> buildAndContinue(planet, startedBuildingAt)
          case BuilderResult.Waiting(waitingTime)
              if timeDiff(clock.now(), waitingTime) < fsConfig.maxBuildingTime && timeDiff(startedBuildingAt, clock.now()) < fsConfig.maxWaitTime =>
            Logger[Task].info(s"Decided to wait for resources to produce til $waitingTime") >>
              executor.waitTo(waitingTime) >> buildAndContinue(planet, startedBuildingAt)
          case _ =>
            Task.unit
        }
    } else {
      Task.unit
    }
  }
}
