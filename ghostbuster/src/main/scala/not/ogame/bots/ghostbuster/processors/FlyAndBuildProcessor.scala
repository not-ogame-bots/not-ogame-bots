package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import cats.implicits._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.ghostbuster.executor._
import not.ogame.bots.ghostbuster.ogame.OgameAction
import not.ogame.bots.ghostbuster.FLogger

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
        .flatMap(planets => withRetry(loop(planets))("flyAndBuild"))
    } else {
      Task.never
    }
  }

  private def loop(planets: List[PlayerPlanet]): Task[Unit] = {
    lookAtInTheAir(planets)
      .flatMap {
        case Some(fleet) => executor.waitTo(fleet.arrivalTime) >> buildAndSend(planets.find(_.coordinates == fleet.to).get, planets)
        case None =>
          lookOnPlanets(planets)
            .flatMap {
              case Some(planet) => buildAndSend(planet, planets)
              case None =>
                Task
                  .eval(clock.now().plus(fsConfig.searchInterval))
                  .flatTap { nextInterval =>
                    Logger[Task].warn(s"Couldn't find fs fleet either on planet or in the air, waiting to the next interval $nextInterval")
                  }
                  .flatMap(nextInterval => executor.waitTo(nextInterval))
            }
      }
      .flatMap(_ => loop(planets))
  }

  private def lookAtInTheAir(planets: List[PlayerPlanet]): Task[Option[MyFleet]] = {
    (for {
      fleetPageData <- ogameDriver.readMyFleets()
      possibleFsFleets = fleetPageData.fleets.find(f => isFsFleet(planets, f))
      waitingTime <- possibleFsFleets match {
        case Some(fleet) =>
          Logger[OgameAction]
            .info("Found fs fleet, waiting for it to reach its target.")
            .as(Option(fleet))
        case None =>
          Logger[OgameAction]
            .warn(s"Couldn't find fs fleet in the air")
            .as(Option.empty[MyFleet])
      }
    } yield waitingTime)
      .execute()
  }

  private def isFsFleet(planets: List[PlayerPlanet], f: MyFleet) = {
    val ships = fsConfig.ships.map(f => f.shipType -> f.amount).toMap
    val eligiblePlanets = fsConfig.eligiblePlanets.map(pId => planets.find(_.id == pId).get)
    f.fleetMissionType == FleetMissionType.Deployment &&
    eligiblePlanets.exists(p => p.coordinates == f.to) &&
    eligiblePlanets.exists(p => p.coordinates == f.from) &&
    f.ships.forall { case (shipType, amount) => amount >= ships(shipType) }
  }

  private def lookOnPlanets(planets: List[PlayerPlanet]): Task[Option[PlayerPlanet]] = {
    Stream
      .emits(planets)
      .evalMap(p => ogameDriver.readFleetPage(p.id).map(p -> _))
      .collectFirst { case (p, f) if isFsFleet(f.ships) => p }
      .compile
      .last
      .execute()
      .flatMap {
        case Some(planet) =>
          Logger[Task].info(s"Planet with fs fleet ${pprint.apply(planet)}").as(Some(planet))
        case None =>
          Logger[Task].warn("Couldn't find fs fleet on any planet").as(None)
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
        .void
    } else {
      Task.unit
    }
  }
}
case class FsConfig(
    ships: List[FleetShip],
    isOn: Boolean,
    searchInterval: FiniteDuration,
    remainDeuterAmount: Int,
    takeResources: Boolean,
    gatherShips: Boolean,
    fleetSpeed: FleetSpeed,
    eligiblePlanets: List[PlanetId],
    builder: Boolean,
    maxWaitTime: FiniteDuration,
    maxBuildingTime: FiniteDuration
)

case class FleetShip(shipType: ShipType, amount: Int)
