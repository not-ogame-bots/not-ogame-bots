package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import cats.implicits._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.ghostbuster.{FLogger, FsConfig, PlanetFleet}

import scala.concurrent.duration._

class FlyAndBuildProcessor(taskExecutor: TaskExecutor, fsConfig: FsConfig, builder: Builder)(implicit clock: LocalClock) extends FLogger {
  def run(): Task[Unit] = {
    if (fsConfig.isOn) {
      taskExecutor
        .readPlanetsAndMoons()
        .map(_.filter(p => fsConfig.eligiblePlanets.contains(p.id)))
        .flatMap(lookOnPlanets)
        .onError(e => Logger[Task].error(s"restarting fly processor ${e.getMessage}"))
        .onErrorRestartIf(_ => true)
    } else {
      Task.never
    }
  }

  private def lookAtInTheAir(planets: List[PlayerPlanet]): Task[Unit] = {
    for {
      fleets <- taskExecutor.readAllFleets()
      possibleFsFleets = fleets.filter(f => isFsFleet(planets, f))
      _ <- possibleFsFleets match {
        case l @ _ :: _ =>
          Logger[Task].info("Too many fleets in the air. Waiting for the first one to reach its target.") >>
            taskExecutor.waitTo(l.map(_.arrivalTime).min) >> lookOnPlanets(planets)
        case Nil =>
          Logger[Task].warn(s"Couldn't find fs fleet either on planets or in the air. Waiting ${fsConfig.searchInterval}...") >>
            Task.sleep(fsConfig.searchInterval) >> lookOnPlanets(planets)
      }
    } yield ()
  }

  private def isFsFleet(planets: List[PlayerPlanet], f: Fleet) = {
    val eligiblePlanets = fsConfig.eligiblePlanets.map(pId => planets.find(_.id == pId).get)
    f.fleetAttitude == FleetAttitude.Friendly && f.fleetMissionType == FleetMissionType.Deployment && eligiblePlanets
      .exists(p => p.coordinates == f.to) && eligiblePlanets.exists(p => p.coordinates == f.from)
  }

  private def lookOnPlanets(planets: List[PlayerPlanet]): Task[Unit] = {
    Stream
      .emits(planets)
      .evalMap(taskExecutor.getFleetOnPlanet)
      .collectFirst { case p if isFsFleet(p) => p }
      .compile
      .last
      .flatMap {
        case Some(planet) =>
          Logger[Task].info(s"Planet with fs fleet ${pprint.apply(planet)}") >>
            buildAndSend(planet.playerPlanet, planets)
        case None =>
          Logger[Task].warn("Couldn't find fs fleet on any planet, looking in the air...") >>
            lookAtInTheAir(planets)
      }
  }

  private def isFsFleet(planetFleet: PlanetFleet): Boolean = {
    fsConfig.ships.forall(fsShip => fsShip.amount <= planetFleet.fleet(fsShip.shipType)) //TODO display unmatched pairs
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
        .flatTap(_ => Logger[Task].info("Fleet sent"))
        .recoverWith {
          case AvailableDeuterExceeded(requiredAmount) =>
            taskExecutor
              .readSupplyPage(from)
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
        new ResourceSelector[Task](deuteriumSelector = Selector.decreaseBy(fsConfig.remainDeuterAmount)).selectResources(taskExecutor, from)
      } else {
        Resources.Zero.pure[Task]
      }
      _ <- taskExecutor
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
        .flatMap {
          case BuilderResult.Building(finishTime)
              if timeDiff(clock.now(), finishTime) < fsConfig.maxBuildingTime && timeDiff(startedBuildingAt, clock.now()) < fsConfig.maxWaitTime =>
            Logger[Task].info(s"Decided to wait for building to finish til $finishTime") >>
              taskExecutor.waitTo(finishTime) >> buildAndContinue(planet, startedBuildingAt)
          case BuilderResult.Waiting(finishTime)
              if timeDiff(clock.now(), finishTime) < fsConfig.maxBuildingTime && timeDiff(startedBuildingAt, clock.now()) < fsConfig.maxWaitTime =>
            Logger[Task].info(s"Decided to wait for building to finish til $finishTime") >>
              taskExecutor.waitTo(finishTime) >> buildAndContinue(planet, startedBuildingAt)
          case BuilderResult.Idle =>
            Task.unit
        }
    } else {
      Task.unit
    }
  }
}
