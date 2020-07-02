package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots.ShipType._
import not.ogame.bots._
import not.ogame.bots.ghostbuster.executor._
import not.ogame.bots.ghostbuster.ogame.OgameAction
import not.ogame.bots.ghostbuster.{ExpeditionConfig, FLogger}

class ExpeditionProcessor(config: ExpeditionConfig, ogameDriver: OgameDriver[OgameAction])(
    implicit executor: OgameActionExecutor[Task],
    clock: LocalClock
) extends FLogger {
  def run(): Task[Unit] = {
    if (config.isOn) {
      ogameDriver
        .readPlanets()
        .execute()
        .map(planets => planets.filter(p => config.startingPlanetId.contains(p.id)).head)
        .flatMap(processAndWait)
    } else {
      Task.never
    }
  }

  private def processAndWait(playerPlanet: PlayerPlanet): Task[Unit] = {
    for {
      time <- lookForFleet(playerPlanet).execute()
      _ <- executor.waitTo(time)
      _ <- withRetry(processAndWait(playerPlanet))("expedition")
    } yield ()
  }

  private def lookForFleet(planet: PlayerPlanet): OgameAction[ZonedDateTime] = {
    for {
      myFleets <- ogameDriver.readMyFleets()
      fleetOnPlanet <- ogameDriver.readFleetPage(planet.id)
      expeditions = myFleets.fleets.filter(_.fleetMissionType == FleetMissionType.Expedition)
      time <- if (expeditions.size < config.maxNumberOfExpeditions) {
        Logger[OgameAction]
          .info(s"Only ${expeditions.size}/${config.maxNumberOfExpeditions} expeditions are in the air")
          .flatMap { _ =>
            val flyingSmallCargoCount = expeditions.map(_.ships(SmallCargoShip)).sum
            val flyingLargeCargoCount = expeditions.map(_.ships(LargeCargoShip)).sum
            val flyingExplorerCount = expeditions.map(_.ships(Explorer)).sum
            val smallCargoToSend =
              Math.min(
                fleetOnPlanet.ships(SmallCargoShip) + flyingSmallCargoCount / config.maxNumberOfExpeditions,
                config.maxSC
              )
            val largeCargoToSend =
              Math.min(
                (fleetOnPlanet.ships(LargeCargoShip) + flyingLargeCargoCount) / config.maxNumberOfExpeditions,
                config.maxLC
              )
            val explorerToSend =
              Math.min((fleetOnPlanet.ships(Explorer) + flyingExplorerCount) / config.maxNumberOfExpeditions, config.maxExplorers)
            val topBattleShip = getTopBattleShip(fleetOnPlanet)
            sendExpedition(
              request = SendFleetRequest(
                planet,
                SendFleetRequestShips.Ships(
                  Map(
                    SmallCargoShip -> Math.min(smallCargoToSend, fleetOnPlanet.ships(ShipType.SmallCargoShip)),
                    LargeCargoShip -> Math.min(largeCargoToSend, fleetOnPlanet.ships(ShipType.LargeCargoShip)),
                    Explorer -> Math.min(explorerToSend, fleetOnPlanet.ships(ShipType.Explorer)),
                    topBattleShip -> 1,
                    EspionageProbe -> Math.min(1, fleetOnPlanet.ships(ShipType.EspionageProbe))
                  )
                ),
                config.target,
                FleetMissionType.Expedition,
                FleetResources.Given(Resources.Zero)
              )
            ).as(clock.now())
          }
      } else {
        val min = expeditions.map(_.arrivalTime).min
        for {
          _ <- collectDebris(List(planet), expeditions) //TODO fixme
          _ <- Logger[OgameAction].info(s"All expeditions are in the air, waiting for first to reach its target - $min")
        } yield min
      }
    } yield time
  }

  private def getTopBattleShip(fleetOnPlanet: FleetPageData): ShipType = {
    if (fleetOnPlanet.ships(Destroyer) > 0) {
      Destroyer
    } else if (fleetOnPlanet.ships(Battleship) > 0) {
      Battleship
    } else if (fleetOnPlanet.ships(Cruiser) > 0) {
      Cruiser
    } else {
      LightFighter
    }
  }

  private def collectDebris(planets: List[PlayerPlanet], expeditions: List[MyFleet]) = {
    val shouldCollectDebris = config.collectingOn && false //TODO fixme
    if (shouldCollectDebris) {
      val debrisCollectingPlanet = planets.filter(_.id == config.collectingPlanet).head
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
                    config.target.copy(coordinatesType = CoordinatesType.Debris),
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

  private def sendExpedition(request: SendFleetRequest) = {
    (for {
      _ <- Logger[OgameAction].info("Sending fleet...")
      _ <- ogameDriver.sendFleet(request)
      _ <- Logger[OgameAction].info("Fleet sent")
    } yield ())
      .recoverWith {
        case AvailableDeuterExceeded(requiredAmount) =>
          Logger[OgameAction].info(s"There was not enough deuter($requiredAmount), expedition won't be send this time")
      }
  }
}
