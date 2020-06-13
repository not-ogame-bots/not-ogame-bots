package not.ogame.bots.ghostbuster.executor

import java.time.ZonedDateTime

import cats.effect.concurrent.MVar
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import monix.reactive.subjects.ConcurrentSubject
import not.ogame.bots._
import not.ogame.bots.ghostbuster.processors.TaskExecutor
import not.ogame.bots.ghostbuster.{FLogger, PlanetFleet}

import scala.concurrent.duration._
import scala.jdk.DurationConverters._

class TaskExecutorImpl(ogameDriver: OgameDriver[Task], clock: LocalClock) extends TaskExecutor with FLogger with StrictLogging {
  type Channel[A] = MVar[Task, A]

  private val responses: Channel[Response] = MVar[Task].empty[Response].runSyncUnsafe()
  private val requests: Channel[Action[_]] = MVar[Task].empty[Action[_]].runSyncUnsafe()
  private val notifications = ConcurrentSubject.publish[Notification]

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
      .flatMap(response => Logger[Task].debug(s"action response: ${pprint.apply(response)}") >> responses.put(response))
      .handleErrorWith { e =>
        for {
          _ <- Task.fromFuture(notifications.onNext(Notification.Failure(e)))
          _ <- Logger[Task].error(e)(e.getMessage)
          isStillLogged <- ogameDriver.checkIsLoggedIn()
          _ <- if (isStillLogged) {
            val response = action.failure(e)
            Logger[Task].warn("still logged, failing action...") >>
              Logger[Task].debug(s"action response: ${pprint.apply(response)}") >>
              responses.put(response)
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
      .flatTap(_ => Task.fromFuture(notifications.onNext(Notification.Login())))
      .handleErrorWith { e =>
        Logger[Task].error(e)("Login failed, retrying in 2 seconds") >> Task.sleep(2 seconds) >> Task.raiseError(e)
      }
      .onErrorRestart(5)
  }

  private def handleAction(action: Action[_]) = {
    action match {
      case a @ Action.BuildSupply(suppliesBuilding, _, planet, _) =>
        ogameDriver
          .buildSuppliesBuilding(planet.id, suppliesBuilding)
          .flatMap(
            _ =>
              ogameDriver
                .readSuppliesPage(planet.id)
                .flatTap(v => Task.fromFuture(notifications.onNext(Notification.SuppliesPageDateRefreshed(v, planet))))
                .map(_.currentBuildingProgress.get)
                .flatTap(time => Task.fromFuture(notifications.onNext(Notification.SupplyBuilt(time.finishTimestamp))))
          )
          .map(buildingProgress => a.success(buildingProgress.finishTimestamp))
      case a @ Action.BuildFacility(facilityBuilding, _, planet, _) =>
        ogameDriver
          .buildFacilityBuilding(planet.id, facilityBuilding)
          .flatMap(
            _ =>
              ogameDriver
                .readFacilityPage(planet.id)
                .flatTap(facilities => Task.fromFuture(notifications.onNext(Notification.FacilityPageDataRefreshed(facilities, planet))))
                .map(_.currentBuildingProgress.get)
                .flatTap(time => Task.fromFuture(notifications.onNext(Notification.FacilityBuilt(time.finishTimestamp))))
          )
          .map(buildingProgress => a.success(buildingProgress.finishTimestamp))
      case a @ Action.ReadSupplyPage(planet, _) =>
        ogameDriver
          .readSuppliesPage(planet.id)
          .flatTap(supplies => Task.fromFuture(notifications.onNext(Notification.SuppliesPageDateRefreshed(supplies, planet))))
          .map(sp => a.success(sp))
      case a @ Action.ReadFacilityPage(planet, _) =>
        ogameDriver
          .readFacilityPage(planet.id)
          .flatTap(facilities => Task.fromFuture(notifications.onNext(Notification.FacilityPageDataRefreshed(facilities, planet))))
          .map(fp => a.success(fp))
      case a @ Action.RefreshFleetOnPlanetStatus(planet, _) =>
        ogameDriver
          .readFleetPage(planet.id)
          .flatTap(fleet => Task.fromFuture(notifications.onNext(Notification.FleetOnPlanetRefreshed(fleet, planet))))
          .map(f => a.success(PlanetFleet(planet, f.ships)))
      case a @ Action.BuildShip(amount, shipType, planet, _) =>
        ogameDriver
          .buildShips(planet.id, shipType, amount)
          .flatMap(_ => ogameDriver.readSuppliesPage(planet.id))
          .flatTap(
            sp =>
              Task.fromFuture(
                notifications.onNext(Notification.ShipBuilt(shipType, amount, planet, sp.currentShipyardProgress.map(_.finishTimestamp)))
              )
          )
          .flatTap(supplies => Task.fromFuture(notifications.onNext(Notification.SuppliesPageDateRefreshed(supplies, planet))))
          .map(sp => a.success(sp))
      case a @ Action.SendFleet(sendFleetRequest, _) =>
        ogameDriver.sendFleet(sendFleetRequest).flatMap { _ =>
          ogameDriver
            .readAllFleets()
            .map { fleets =>
              fleets
                .collect { case f if isSameFleet(sendFleetRequest, f) => f }
                .maxBy(_.arrivalTime)
            }
            .flatTap(fleet => Task.fromFuture(notifications.onNext(Notification.FleetSent(sendFleetRequest, fleet.arrivalTime))))
            .flatTap(_ => ogameDriver.readPlanets().flatMap(p => Task.fromFuture(notifications.onNext(Notification.ReadPlanets(p)))))
            .map(f => a.success(f.arrivalTime))
        }
      case a: Action.GetAirFleet =>
        ogameDriver
          .readAllFleets()
          .flatTap(fleets => Task.fromFuture(notifications.onNext(Notification.GetAirFleet(fleets))))
          .flatTap(_ => ogameDriver.readPlanets().flatMap(p => Task.fromFuture(notifications.onNext(Notification.ReadPlanets(p)))))
          .map(fleets => a.success(fleets))
      case a: Action.ReadPlanets =>
        ogameDriver
          .readPlanets()
          .flatTap(p => Task.fromFuture(notifications.onNext(Notification.ReadPlanets(p))))
          .map(planets => a.success(planets))
      case a @ Action.ReturnFleetAction(id, _) =>
        ogameDriver.returnFleet(id) >> Task.sleep(1 seconds) >> ogameDriver
          .readMyFleets()
          .flatTap(myFleets => Task.fromFuture(notifications.onNext(Notification.ReadMyFleetAction(myFleets))))
          .map(_.find(_.fleetId == id).get.arrivalTime)
          .flatTap(time => Task.fromFuture(notifications.onNext(Notification.ReturnFleetAction(id, time))))
          .map(a.success)
      case a: Action.ReadMyFleetAction =>
        ogameDriver
          .readMyFleets()
          .flatTap(fleets => Task.fromFuture(notifications.onNext(Notification.ReadMyFleetAction(fleets))))
          .map(r => a.success(r))
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
      Task.sleep(sleepTime.plus(2 seconds)) // additional 1 seconds to make things go smooth
  }

  private def calculateSleepTime(futureTime: ZonedDateTime) = {
    java.time.Duration.between(clock.now(), futureTime).toScala
  }

  override def readAllFleets(): Task[List[Fleet]] = {
    val action = Action.GetAirFleet()
    exec(action)
  }

  override def readPlanetsAndMoons(): Task[List[PlayerPlanet]] = {
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
    val action = Action.ReadSupplyPage(playerPlanet)
    exec(action)
  }

  override def readFacilityPage(playerPlanet: PlayerPlanet): Task[FacilityPageData] = {
    val action = Action.ReadFacilityPage(playerPlanet)
    exec(action)
  }

  override def buildSupplyBuilding(
      suppliesBuilding: SuppliesBuilding,
      level: Int Refined Positive,
      planet: PlayerPlanet
  ): Task[ZonedDateTime] = {
    val action = Action.BuildSupply(suppliesBuilding, level, planet)
    exec(action)
  }

  override def buildFacilityBuilding(
      facilityBuilding: FacilityBuilding,
      level: Refined[Int, Positive],
      planet: PlayerPlanet
  ): Task[ZonedDateTime] = {
    val action = Action.BuildFacility(facilityBuilding, level, planet)
    exec(action)
  }

  override def buildShip(shipType: ShipType, amount: Int, planet: PlayerPlanet): Task[SuppliesPageData] = {
    val action = Action.BuildShip(amount, shipType, planet)
    exec(action)
  }

  override def returnFleet(fleetId: FleetId): Task[ZonedDateTime] = {
    val action = Action.ReturnFleetAction(fleetId)
    exec(action)
  }

  override def readMyFleets(): Task[List[MyFleet]] = {
    val action = Action.ReadMyFleetAction()
    exec(action)
  }

  private val singleThreadExecution = Scheduler.singleThread("taskExecutor")

  private def exec[T](action: Action[T]) = {
    (requests.put(action) >>
      responses.take
        .flatMap {
          case r @ Response.Success(anyValue, _) =>
            val value = action.defer(anyValue)
            Task.pure(value)
          case Response.Failure(_, uuid) =>
            Task.raiseError[T](new RuntimeException(s"Couldn't execute operation $uuid"))
        }).executeOn(singleThreadExecution)
  }

  override def subscribeToNotifications: Observable[Notification] = notifications
}
