package not.ogame.bots.ghostbuster.executor

import java.time.ZonedDateTime

import cats.effect.concurrent.MVar
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import not.ogame.bots._
import not.ogame.bots.ghostbuster.processors.TaskExecutor
import not.ogame.bots.ghostbuster.{FLogger, PlanetFleet}

import scala.concurrent.duration._
import scala.jdk.DurationConverters._

class TaskExecutorImpl(ogameDriver: OgameDriver[Task], clock: LocalClock) extends TaskExecutor with FLogger with StrictLogging {
  type Channel[A] = MVar[Task, A]

  private val responses = MVar[Task].empty[Response].runSyncUnsafe()
  private val requests = MVar[Task].empty[Action[_]].runSyncUnsafe()

  def run(): Task[Unit] = {
    safeLogin() >> processNextAction()
  }

  private def processNextAction(): Task[Unit] = {
    for {
      action <- requests.take
      _ <- Logger[Task].debug(s"executing action: ${pprint.apply(action)}")
      _ <- safeHandleAction(action)
      _ <- processNextAction()
    } yield ()
  }

  private def safeHandleAction(action: Action[_]): Task[Unit] = {
    handleAction(action)
      .flatMap(response => responses.put(response))
      .handleErrorWith { e =>
        for {
          _ <- Logger[Task].error(e)(e.getMessage)
          isStillLogged <- ogameDriver.checkIsLoggedIn()
          _ <- if (isStillLogged) {
            Logger[Task].warn("still logged, failing action...") >>
              responses.put(action.failure(e)) >> processNextAction()
          } else {
            Logger[Task].warn("not logged") >>
              safeLogin >> safeHandleAction(action)
          }
        } yield ()
      }
  }

  private def safeLogin(): Task[Unit] = {
    ogameDriver
      .login()
      .handleErrorWith { e =>
        Logger[Task].error(e)("Login failed, retrying in 2 seconds") >> Task.sleep(2 seconds) >> Task.raiseError(e)
      }
      .onErrorRestart(5)
  }

  private def handleAction(action: Action[_]) = {
    action match {
      case a @ Action.BuildSupply(suppliesBuilding, _, planetId) =>
        ogameDriver
          .buildSuppliesBuilding(planetId, suppliesBuilding)
          .flatMap(_ => ogameDriver.readSuppliesPage(planetId).map(_.currentBuildingProgress.get))
          .map(buildingProgress => a.success(buildingProgress.finishTimestamp))
      case a @ Action.BuildFacility(facilityBuilding, _, planetId) =>
        ogameDriver
          .buildFacilityBuilding(planetId, facilityBuilding)
          .flatMap(_ => ogameDriver.readFacilityPage(planetId).map(_.currentBuildingProgress.get))
          .map(buildingProgress => a.success(buildingProgress.finishTimestamp))
      case a @ Action.ReadSupplyPage(planetId) =>
        ogameDriver
          .readSuppliesPage(planetId)
          .map(sp => a.success(sp))
      case a @ Action.ReadFacilityPage(planetId) =>
        ogameDriver
          .readFacilityPage(planetId)
          .map(fp => a.success(fp))
      case a @ Action.RefreshFleetOnPlanetStatus(planetId) =>
        ogameDriver
          .checkFleetOnPlanet(planetId.id)
          .map(f => a.success(PlanetFleet(planetId, f)))
      case a @ Action.BuildShip(amount, shipType, planetId) =>
        ogameDriver
          .buildShips(planetId, shipType, amount)
          .flatMap(_ => ogameDriver.readSuppliesPage(planetId))
          .map(sp => a.success(sp))
      case a @ Action.SendFleet(sendFleetRequest) =>
        ogameDriver.sendFleet(sendFleetRequest).flatMap { _ =>
          ogameDriver
            .readAllFleets()
            .map { fleets =>
              fleets
                .collect { case f if isSameFleet(sendFleetRequest, f) => f }
                .maxBy(_.arrivalTime)
            } //TODO or min?
            .flatTap(_ => ogameDriver.readPlanets())
            .map(f => a.success(f.arrivalTime))
        }
      case a: Action.GetAirFleet =>
        ogameDriver
          .readAllFleets()
          .flatTap(_ => ogameDriver.readPlanets())
          .map(fleets => a.success(fleets))
      case a: Action.ReadPlanets =>
        ogameDriver
          .readPlanets()
          .map(planets => a.success(planets))
    }
  }

  private def isSameFleet(sendFleetRequest: SendFleetRequest, f: Fleet) = {
    f.to == sendFleetRequest.targetCoordinates &&
    f.fleetMissionType == sendFleetRequest.fleetMissionType &&
    f.from == sendFleetRequest.from.coordinates
  }

  override def waitTo(now: ZonedDateTime): Task[Unit] = {
    val sleepTime = calculateSleepTime(now)
    Logger[Task].debug(s"sleeping ~ ${sleepTime.toSeconds / 60} minutes til $now") >>
      Task.sleep(sleepTime.plus(1 seconds)) // additional 1 seconds to make things go smooth
  }

  private def calculateSleepTime(futureTime: ZonedDateTime) = {
    java.time.Duration.between(clock.now(), futureTime).toScala
  }

  override def readAllFleets(): Task[List[Fleet]] = {
    val action = Action.GetAirFleet()
    exec(action)
  }

  override def readPlanets(): Task[List[PlayerPlanet]] = {
    val action = Action.ReadPlanets()
    exec(action)
  }

  override def sendFleet(req: SendFleetRequest): Task[ZonedDateTime] = {
    val action = Action.SendFleet(req)
    exec(action)
  }

  override def getFleetOnPlanet(planet: PlayerPlanet): Task[PlanetFleet] = {
    val action = Action.RefreshFleetOnPlanetStatus(planet)
    exec(action)
  }

  override def readSupplyPage(playerPlanet: PlayerPlanet): Task[SuppliesPageData] = {
    val action = Action.ReadSupplyPage(playerPlanet.id)
    exec(action)
  }

  override def readFacilityPage(playerPlanet: PlayerPlanet): Task[FacilityPageData] = {
    val action = Action.ReadFacilityPage(playerPlanet.id)
    exec(action)
  }

  override def buildSupplyBuilding(
      suppliesBuilding: SuppliesBuilding,
      level: Int Refined Positive,
      planet: PlayerPlanet
  ): Task[ZonedDateTime] = {
    val action = Action.BuildSupply(suppliesBuilding, level, planet.id)
    exec(action)
  }

  override def buildFacilityBuilding(
      facilityBuilding: FacilityBuilding,
      level: Refined[Int, Positive],
      planet: PlayerPlanet
  ): Task[ZonedDateTime] = {
    val action = Action.BuildFacility(facilityBuilding, level, planet.id)
    exec(action)
  }

  override def buildShip(shipType: ShipType, amount: Int, head: PlayerPlanet): Task[SuppliesPageData] = {
    val action = Action.BuildShip(amount, shipType, head.id)
    exec(action)
  }

  private def exec[T](action: Action[T]) = {
    requests.put(action) >>
      responses.take
        .flatMap {
          case Response.Success(anyValue) =>
            val value = action.defer(anyValue)
            Logger[Task].debug(s"action response: ${pprint.apply(value)}").map(_ => value)
          case Response.Failure(_) =>
            Task.raiseError[T](new RuntimeException("Couldn't execute operation"))
        }
  }
}
