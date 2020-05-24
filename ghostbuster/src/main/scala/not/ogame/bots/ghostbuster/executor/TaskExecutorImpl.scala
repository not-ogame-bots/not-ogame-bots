package not.ogame.bots.ghostbuster.executor

import java.time.{Clock, Instant}

import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import monix.catnap.ConcurrentQueue
import monix.eval.Task
import monix.execution.BufferCapacity
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{Consumer, MulticastStrategy}
import not.ogame.bots._
import not.ogame.bots.ghostbuster.processors.TaskExecutor
import not.ogame.bots.ghostbuster.PlanetFleet

import scala.concurrent.duration.{FiniteDuration, _}

class TaskExecutorImpl(ogameDriver: OgameDriver[Task], clock: Clock) extends TaskExecutor {
  private val subject = ConcurrentSubject(MulticastStrategy.publish[Response])
  private val queue = ConcurrentQueue[Task].unsafe[Action[_]](BufferCapacity.Unbounded())

  def run(): Task[Unit] = {
    safeLogin() >> processNextAction()
  }

  private def processNextAction(): Task[Unit] = {
    for {
      action <- queue.poll
      _ <- Task.eval(println(s"executing action: ${pprint.apply(action)}"))
      _ <- safeHandleAction(action)
      _ <- processNextAction()
    } yield ()
  }

  private def safeHandleAction(action: Action[_]): Task[Unit] = {
    handleAction(action).void
      .handleErrorWith { e =>
        e.printStackTrace()
        safeLogin >> safeHandleAction(action)
      }
  }

  private def safeLogin(): Task[Unit] = {
    ogameDriver
      .login()
      .handleErrorWith { e =>
        e.printStackTrace()
        Task.eval(println("Login failed, retrying in 10 seconds")) >> Task.sleep(10 seconds) >> safeLogin()
      }
  }

  private def handleAction(action: Action[_]) = {
    action match {
      case a @ Action.BuildSupply(suppliesBuilding, level, executionTime, planetId, uuid) =>
        ogameDriver
          .buildSuppliesBuilding(planetId, suppliesBuilding)
          .flatMap { _ =>
            ogameDriver.readSuppliesPage(planetId).map(_.currentBuildingProgress.get)
          }
          .flatMap { suppliesPage =>
            Task.fromFuture(subject.onNext(a.response(suppliesPage.finishTimestamp)))
          }
      case a @ Action.BuildFacility(suppliesBuilding, level, executionTime, planetId, uuid) =>
        ogameDriver
          .buildFacilityBuilding(planetId, suppliesBuilding)
          .flatMap { _ =>
            ogameDriver.readFacilityBuildingsLevels(planetId)
          }
          .flatMap { sp =>
            Task.from(subject.onNext(a.response(sp)))
          }
      case a @ Action.RefreshSupplyAndFacilityPage(executionTime, planetId, uuid) =>
        Task.unit //TODO? deliberately
      case a @ Action.ReadSupplyPage(executionTime, planetId, uuid) =>
        ogameDriver.readSuppliesPage(planetId).flatMap(sp => Task.fromFuture(subject.onNext(a.response(sp))))
      case a @ Action.RefreshFleetOnPlanetStatus(executionTime, planetId, uuid) =>
        ogameDriver
          .checkFleetOnPlanet(planetId.id)
          .flatMap(f => Task.fromFuture(subject.onNext(a.response(PlanetFleet(planetId, f)))))
      case a @ Action.BuildShip(amount, shipType, executionTime, planetId, uuid) =>
        ogameDriver
          .buildShips(planetId, shipType, amount)
          .flatMap { _ =>
            ogameDriver.readSuppliesPage(planetId)
          }
          .flatMap { sp =>
            Task.fromFuture(subject.onNext(a.response(sp)))
          }
      case a @ Action.SendFleet(executionTime, sendFleetRequest, uuid) =>
        ogameDriver.sendFleet(sendFleetRequest).flatMap { _ =>
          ogameDriver
            .readAllFleets()
            .map { fleets =>
              fleets.find(f => f.to == sendFleetRequest.targetCoordinates && f.fleetMissionType == sendFleetRequest.fleetMissionType).get
            }
            .flatMap(f => Task.fromFuture(subject.onNext(a.response(f.arrivalTime))))
        }
      case a @ Action.GetAirFleet(executionTime, uuid) =>
        ogameDriver.readAllFleets().flatMap { fleets =>
          Task.fromFuture(subject.onNext(a.response(fleets)))
        } >> ogameDriver.readPlanets()
      case a @ Action.ReadPlanets(executionTime, uuid) =>
        ogameDriver.readPlanets().flatMap { planets =>
          Task.fromFuture(subject.onNext(a.response(planets)))
        }
    }
  }

  override def waitSeconds(duration: FiniteDuration): Task[Unit] = Task.sleep(duration)

  override def readAllFleets(): Task[List[Fleet]] = {
    val action = Action.GetAirFleet(clock.instant())
    exec(action)
  }

  override def readPlanets(): Task[List[PlayerPlanet]] = {
    val action = Action.ReadPlanets(clock.instant())
    exec(action)
  }

  override def sendFleet(req: SendFleetRequest): Task[Instant] = {
    val action = Action.SendFleet(clock.instant(), req)
    exec(action)
  }

  override def getFleetOnPlanet(planet: PlayerPlanet): Task[PlanetFleet] = {
    val action = Action.RefreshFleetOnPlanetStatus(clock.instant(), planet)
    exec(action)
  }

  override def readSupplyPage(playerPlanet: PlayerPlanet): Task[SuppliesPageData] = {
    val action = Action.ReadSupplyPage(clock.instant(), playerPlanet.id)
    exec(action)
  }

  override def buildSupplyBuilding(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, planet: PlayerPlanet): Task[Instant] = {
    val action = Action.BuildSupply(suppliesBuilding, level, clock.instant(), planet.id)
    exec(action)
  }

  private def exec[T](action: Action[T]) = {
    queue.tryOffer(action) >> subject
      .collect { case r if r.uuid == action.uuid => action.defer(r.value) }
      .consumeWith(Consumer.head)
  }
}
