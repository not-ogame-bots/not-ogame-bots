package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.ghostbuster.{FLogger, FsConfig, PlanetFleet}
import fs2.Stream
import scala.concurrent.duration.{FiniteDuration, _}
import scala.jdk.DurationConverters._

class FlyAndBuildProcessor(taskExecutor: TaskExecutor, fsConfig: FsConfig, builder: Builder)(implicit clock: LocalClock) extends FLogger {
  def run(): Task[Unit] = {
    if (fsConfig.isOn) {
      taskExecutor
        .readPlanets()
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
            taskExecutor.waitTo(l.map(_.arrivalTime).min) >> lookOnPlanets(planets) //TODO look on single planet
        case Nil =>
          Logger[Task].warn(s"Couldn't find fs fleet either on planets or in the air. Waiting ${fsConfig.searchInterval}...") >>
            Task.sleep(fsConfig.searchInterval) >> lookOnPlanets(planets)
      }
    } yield ()
  }

  private def isFsFleet(planets: List[PlayerPlanet], f: Fleet) = {
    f.fleetAttitude == FleetAttitude.Friendly && f.fleetMissionType == FleetMissionType.Deployment && planets
      .exists(p => p.coordinates == f.to) && planets.exists(p => p.coordinates == f.from)
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
      arrivalTime <- sendFleet(from = currentPlanet, to = targetPlanet)
      _ <- Logger[Task].info(s"Waiting for fleet to arrive til $arrivalTime")
      _ <- taskExecutor.waitTo(arrivalTime)
      _ <- buildAndSend(currentPlanet = targetPlanet, planets)
    } yield ()
  }

  private def nextPlanet(currentPlanet: PlayerPlanet, planets: List[PlayerPlanet]) = {
    val idx = (planets.indexOf(currentPlanet) + 1) % planets.size
    planets(idx)
  }

  private def sendFleet(from: PlayerPlanet, to: PlayerPlanet): Task[ZonedDateTime] = {
    for {
      _ <- Logger[Task].info("Sending fleet...")
      suppliesPageData <- taskExecutor.readSupplyPage(from)
      arrivalTime <- if (suppliesPageData.currentResources.deuterium >= fsConfig.deuterThreshold) {
        sendFleetImpl(from, to)
      } else {
        val missingDeuter = fsConfig.deuterThreshold - suppliesPageData.currentResources.deuterium
        val timeToProduceInHours = missingDeuter.toDouble / suppliesPageData.currentProduction.deuterium
        val timeInSeconds = (timeToProduceInHours * 60 * 60).toInt
        Logger[Task].info(s"There was not enough deuter, fleet sending delayed by $timeInSeconds seconds") >> Task.sleep(
          timeInSeconds seconds
        ) >> sendFleet(from, to)
      }
    } yield arrivalTime
  }

  private def sendFleetImpl(from: PlayerPlanet, to: PlayerPlanet) = {
    for {
      resources <- if (fsConfig.takeResources) {
        new ResourceSelector[Task](deuteriumSelector = Selector.decreaseBy(fsConfig.remainDeuterAmount)).selectResources(taskExecutor, from)
      } else {
        Resources.Zero.pure[Task]
      }
      arrivalTime <- taskExecutor
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
    } yield arrivalTime
  }

  private def buildAndContinue(planet: PlayerPlanet, startedBuildingAt: ZonedDateTime): Task[Unit] = {
    if (fsConfig.builder) {
      builder.buildNextThingFromWishList(planet).flatMap {
        case Some(finishTime)
            if timeDiff(clock.now(), finishTime) < fsConfig.maxBuildingTime && timeDiff(startedBuildingAt, clock.now()) < fsConfig.maxWaitTime =>
          Logger[Task].info(s"Decided to wait for building to finish til $finishTime") >>
            taskExecutor.waitTo(finishTime) >> buildAndContinue(planet, startedBuildingAt)
        case _ => Task.unit
      }
    } else {
      Task.unit
    }
  }

  private def timeDiff(earlier: ZonedDateTime, later: ZonedDateTime): FiniteDuration = {
    java.time.Duration.between(earlier, later).toScala
  }
}
