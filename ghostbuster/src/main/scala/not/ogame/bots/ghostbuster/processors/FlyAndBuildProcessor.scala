package not.ogame.bots.ghostbuster.processors

import java.time.{Clock, Instant, ZonedDateTime, Period}
import java.util.concurrent.TimeUnit

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.ghostbuster.{BotConfig, FLogger, PlanetFleet}
import scala.jdk.DurationConverters._
import scala.concurrent.duration.{FiniteDuration, _}

class FlyAndBuildProcessor(taskExecutor: TaskExecutor, botConfig: BotConfig, clock: LocalClock) extends FLogger {
  private val builder = new Builder(taskExecutor, botConfig)

  def run(): Task[Unit] = {
    if (botConfig.fsConfig.isOn) {
      for {
        planets <- taskExecutor.readPlanets()
        fleets <- taskExecutor.readAllFleets()
        _ <- fleets.find( //TODO if there is more than one fleet should wait
          f =>
            f.fleetAttitude == FleetAttitude.Friendly && f.fleetMissionType == FleetMissionType.Deployment && planets
              .exists(p => p.coordinates == f.to) && planets.exists(p => p.coordinates == f.from)
        ) match {
          case Some(fleet) =>
            Logger[Task].info(s"Found our fleet in the air: ${pprint.apply(fleet)}").flatMap { _ =>
              val toPlanet = planets.find(p => fleet.to == p.coordinates).get
              taskExecutor.waitTo(fleet.arrivalTime) >> buildAndSend(toPlanet, planets) // TODO if it is returning then from!!
            }
          case None => lookAndSend(planets)
        }
      } yield ()
    } else {
      Task.never
    }
  }

  private def lookAndSend(planets: List[PlayerPlanet]): Task[Unit] = {
    val planetWithFsFleet = planets
      .map { planet =>
        taskExecutor.getFleetOnPlanet(planet)
      }
      .sequence
      .map(planetFleets => planetFleets.find(isFsFleet))

    planetWithFsFleet.flatMap {
      case Some(planet) =>
        println(s"Planet with fs fleet ${pprint.apply(planet)}")
        buildAndSend(planet.playerPlanet, planets)
      case None =>
        println("Couldn't find fs fleet on any planet, retrying in 10 minutes")
        Task.sleep(10 minutes) >> lookAndSend(planets)
    }
  }

  private def isFsFleet(planetFleet: PlanetFleet): Boolean = {
    botConfig.fsConfig.ships.forall(fsShip => fsShip.amount <= planetFleet.fleet(fsShip.shipType))
  }

  private def buildAndSend(currentPlanet: PlayerPlanet, planets: List[PlayerPlanet]): Task[Unit] = {
    val targetPlanet = nextPlanet(currentPlanet, planets)
    for {
      _ <- buildAndContinue(currentPlanet, clock.now())
      _ <- sendFleet(from = currentPlanet, to = targetPlanet)
      _ <- Task.eval("sleeping 2 minutes before taking off") >> Task.sleep(2 minutes)
      _ <- buildAndSend(currentPlanet = targetPlanet, planets)
    } yield ()
  }

  private def nextPlanet(currentPlanet: PlayerPlanet, planets: List[PlayerPlanet]) = {
    val idx = (planets.indexOf(currentPlanet) + 1) % planets.size
    planets(idx)
  }

  private def sendFleet(from: PlayerPlanet, to: PlayerPlanet): Task[Unit] = { //TODO if couldn't take all resources then build mt
    for {
      arrivalTime <- taskExecutor
        .sendFleet(
          SendFleetRequest(
            from,
            SendFleetRequestShips.Ships(botConfig.fsConfig.ships.map(s => s.shipType -> s.amount).toMap),
            to.coordinates,
            FleetMissionType.Deployment,
            if (botConfig.fsConfig.takeResources) {
              FleetResources.Max
            } else {
              FleetResources.Given(Resources.Zero)
            }
          )
        )
      _ <- taskExecutor.waitTo(arrivalTime)
    } yield ()
  }

  private def buildAndContinue(planet: PlayerPlanet, startedBuildingAt: ZonedDateTime): Task[Unit] = { //TODO it should be inside smart builder not outside
    builder.buildNextThingFromWishList(planet).flatMap {
      case Some(elapsedTime)
          if timeDiff(elapsedTime, clock.now()) < (10 minutes) && timeDiff(startedBuildingAt, clock.now()) < (20 minutes) =>
        taskExecutor.waitTo(elapsedTime) >> buildAndContinue(planet, startedBuildingAt)
      case _ => Task.unit
    }
  }

  private def timeDiff(first: ZonedDateTime, second: ZonedDateTime): FiniteDuration = {
    java.time.Duration.between(first, second).toScala
  }
}
