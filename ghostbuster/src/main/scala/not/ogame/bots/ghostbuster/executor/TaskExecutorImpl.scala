package not.ogame.bots.ghostbuster.executor

import java.time.ZonedDateTime
import java.util.UUID

import cats.effect.concurrent.MVar
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import not.ogame.bots._
import not.ogame.bots.ghostbuster.processors.TaskExecutor
import not.ogame.bots.ghostbuster.{FLogger, PlanetFleet, processors}

import scala.concurrent.duration._

class TaskExecutorImpl(ogameDriver: OgameDriver[Task] with NotificationAware)(implicit clock: LocalClock)
    extends TaskExecutor
    with FLogger
    with StrictLogging {
  type Channel[A] = MVar[Task, A]

  private val requests: Channel[Request[_]] = MVar[Task].empty[Request[_]].runSyncUnsafe()

  def run(): Task[Unit] = {
    safeLogin() >> processNextAction()
  }

  private def processNextAction(): Task[Unit] = {
    for {
      action <- requests.take
      _ <- safeHandleAction(action)
      _ <- processNextAction()
    } yield ()
  }

  private def safeHandleAction[T](request: Request[T]): Task[Unit] = {
    handleAction(request)
      .handleErrorWith { e =>
        for {
          _ <- Logger[Task].error(e)(e.getMessage)
          isStillLogged <- ogameDriver.checkIsLoggedIn()
          _ <- if (isStillLogged) {
            val response = Response.Failure[T](e)
            Logger[Task].warn("still logged, failing action...") >>
              Logger[Task].debug(s"action response: ${pprint.apply(response)}") >>
              request.response.put(response)
          } else {
            Logger[Task].warn("not logged") >>
              safeLogin >> safeHandleAction(request)
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

  private def handleAction[T](request: Request[T]) = {
    request.action
      .flatTap(response => Logger[Task].debug(s"action response: ${pprint.apply(response)}"))
      .flatMap(r => request.response.put(Response.success(r)))
  }

  private def isSameFleet(sendFleetRequest: SendFleetRequest, f: Fleet) = {
    f.to == sendFleetRequest.targetCoordinates &&
    f.fleetMissionType == sendFleetRequest.fleetMissionType &&
    f.from == sendFleetRequest.from.coordinates
  }

  override def waitTo(future: ZonedDateTime): Task[Unit] = {
    val sleepTime = processors.timeDiff(clock.now(), future)
    Logger[Task].debug(s"sleeping ~ ${sleepTime.toSeconds / 60} minutes til $future") >>
      Task.sleep(sleepTime.plus(2 seconds)) // additional 1 seconds to make things go smooth
  }

  override def readAllFleets(): Task[List[Fleet]] = {
    exec(
      Logger[Task].debug("readAllFleets") >>
        ogameDriver
          .readAllFleets()
          .flatTap(_ => ogameDriver.readPlanets())
    )
  }

  override def readPlanetsAndMoons(): Task[List[PlayerPlanet]] = {
    exec(
      Logger[Task].debug("readPlanetsAndMoons") >>
        ogameDriver.readPlanets()
    )
  }

  override def sendFleet(req: SendFleetRequest): Task[ZonedDateTime] = {
    exec(
      Logger[Task].debug("sendFleet") >>
        ogameDriver.sendFleet(req).flatMap { _ =>
          ogameDriver
            .readAllFleets()
            .map { fleets =>
              fleets
                .collect { case f if isSameFleet(req, f) => f }
                .maxBy(_.arrivalTime)
            }
            .flatTap(_ => ogameDriver.readPlanets())
            .map(f => f.arrivalTime)
        }
    )
  }

  override def getFleetOnPlanet(planet: PlayerPlanet): Task[PlanetFleet] = {
    exec(
      Logger[Task].debug("getFleetOnPlanet") >>
        ogameDriver
          .readFleetPage(planet.id)
          .map(fp => PlanetFleet(planet, fp.ships))
    )
  }

  override def readSupplyPage(playerPlanet: PlayerPlanet): Task[SuppliesPageData] = {
    exec(
      Logger[Task].debug("readSupplyPage") >>
        ogameDriver.readSuppliesPage(playerPlanet.id)
    )
  }

  override def readFacilityPage(playerPlanet: PlayerPlanet): Task[FacilityPageData] = {
    exec(
      Logger[Task].debug("readFacilityPage") >>
        ogameDriver.readFacilityPage(playerPlanet.id)
    )
  }

  override def buildSupplyBuilding(
      suppliesBuilding: SuppliesBuilding,
      level: Int Refined Positive,
      planet: PlayerPlanet
  ): Task[ZonedDateTime] = {
    exec(
      Logger[Task].debug("buildSupplyBuilding") >>
        ogameDriver
          .buildSuppliesBuilding(planet.id, suppliesBuilding)
          .flatMap(
            _ =>
              ogameDriver
                .readSuppliesPage(planet.id)
                .map(_.currentBuildingProgress.get.finishTimestamp)
          )
    )
  }

  override def buildFacilityBuilding(
      facilityBuilding: FacilityBuilding,
      level: Refined[Int, Positive],
      planet: PlayerPlanet
  ): Task[ZonedDateTime] = {
    exec(
      Logger[Task].debug("buildFacilityBuilding") >>
        ogameDriver
          .buildFacilityBuilding(planet.id, facilityBuilding)
          .flatMap(
            _ =>
              ogameDriver
                .readFacilityPage(planet.id)
                .map(_.currentBuildingProgress.get.finishTimestamp)
          )
    )
  }

  override def buildShip(shipType: ShipType, amount: Int, planet: PlayerPlanet): Task[SuppliesPageData] = {
    exec(
      Logger[Task].debug("buildShip") >>
        ogameDriver
          .buildShips(planet.id, shipType, amount)
          .flatMap(_ => ogameDriver.readSuppliesPage(planet.id))
    )
  }

  override def returnFleet(fleetId: FleetId): Task[ZonedDateTime] = {
    exec(
      Logger[Task].debug("returnFleet") >>
        ogameDriver.returnFleet(fleetId) >>
        Task.sleep(1 seconds) >>
        ogameDriver
          .readMyFleets()
          .map(_.fleets.find(_.fleetId == fleetId).get.arrivalTime)
    )
  }

  override def readMyFleets(): Task[MyFleetPageData] = {
    exec(Logger[Task].debug("readMyFleets") >> ogameDriver.readMyFleets())
  }

  private def exec[T](action: Task[T]) = {
    val uuid = UUID.randomUUID()
    Request[T](Logger[Task].debug(s"start action: $uuid") >> action <* Logger[Task].debug(s"end action $uuid"))
      .flatMap { r =>
        requests.put(r) >>
          r.response.take
            .flatMap {
              case Response.Success(value) =>
                Task.pure(value)
              case Response.Failure(ex) =>
                Task.raiseError[T](ex)
            }
      }
  }

  override def subscribeToNotifications: Observable[Notification] = ogameDriver.subscribeToNotifications
}
