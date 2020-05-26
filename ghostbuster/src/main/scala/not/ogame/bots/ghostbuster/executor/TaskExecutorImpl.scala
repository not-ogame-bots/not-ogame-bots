package not.ogame.bots.ghostbuster.executor

import java.time.ZonedDateTime

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import io.chrisdavenport.log4cats.Logger
import monix.catnap.ConcurrentQueue
import monix.eval.Task
import monix.execution.BufferCapacity
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{Consumer, MulticastStrategy}
import not.ogame.bots._
import not.ogame.bots.ghostbuster.processors.TaskExecutor
import not.ogame.bots.ghostbuster.{FLogger, PlanetFleet}

import scala.concurrent.duration._
import scala.jdk.DurationConverters._

class TaskExecutorImpl(ogameDriver: OgameDriver[Task], clock: LocalClock) extends TaskExecutor with FLogger with StrictLogging {
  private val subject = ConcurrentSubject(MulticastStrategy.publish[Response])
  private val queue = ConcurrentQueue[Task].unsafe[Action[_]](BufferCapacity.Unbounded())

  def run(): Task[Unit] = {
    safeLogin() >> processNextAction()
  }

  private def processNextAction(): Task[Unit] = {
    for {
      action <- queue.poll
      _ <- Logger[Task].debug(s"executing action: ${pprint.apply(action)}")
      _ <- safeHandleAction(action)
      _ <- processNextAction()
    } yield ()
  }

  private def safeHandleAction(action: Action[_]): Task[Unit] = {
    handleAction(action)
      .flatMap(response => Task.fromFuture(subject.onNext(response)).void)
      .handleErrorWith { e =>
        Logger[Task].error(e)(e.getMessage) >>
          safeLogin >>
          safeHandleAction(action)
      }
  }

  private def safeLogin(): Task[Unit] = {
    ogameDriver
      .login()
      .handleErrorWith { e =>
        Logger[Task].error(e)("Login failed, retrying in 10 seconds") >> Task.sleep(10 seconds) >> safeLogin()
      }
  }

  private def handleAction(action: Action[_]) = {
    action match {
      case a @ Action.BuildSupply(suppliesBuilding, _, _, planetId, _) =>
        ogameDriver
          .buildSuppliesBuilding(planetId, suppliesBuilding)
          .flatMap(_ => ogameDriver.readSuppliesPage(planetId).map(_.currentBuildingProgress.get))
          .map(suppliesPage => a.response(suppliesPage.finishTimestamp))
      case a @ Action.BuildFacility(suppliesBuilding, _, _, planetId, _) =>
        ogameDriver
          .buildFacilityBuilding(planetId, suppliesBuilding)
          .flatMap(_ => ogameDriver.readFacilityPage(planetId))
          .map(sp => a.response(sp))
      case a @ Action.ReadSupplyPage(_, planetId, _) =>
        ogameDriver
          .readSuppliesPage(planetId)
          .map(sp => a.response(sp))
      case a @ Action.RefreshFleetOnPlanetStatus(_, planetId, _) =>
        ogameDriver
          .checkFleetOnPlanet(planetId.id)
          .map(f => a.response(PlanetFleet(planetId, f)))
      case a @ Action.BuildShip(amount, shipType, _, planetId, _) =>
        ogameDriver
          .buildShips(planetId, shipType, amount)
          .flatMap(_ => ogameDriver.readSuppliesPage(planetId))
          .map(sp => a.response(sp))
      case a @ Action.SendFleet(_, sendFleetRequest, _) =>
        ogameDriver.sendFleet(sendFleetRequest).flatMap { _ =>
          ogameDriver
            .readAllFleets()
            .map { fleets =>
              fleets
                .collect { case f if isSameFleet(sendFleetRequest, f) => f }
                .maxBy(_.arrivalTime)
            } //TODO or min?
            .flatTap(_ => ogameDriver.readPlanets())
            .map(f => a.response(f.arrivalTime))
        }
      case a @ Action.GetAirFleet(_, _) =>
        ogameDriver
          .readAllFleets()
          .flatTap(_ => ogameDriver.readPlanets())
          .map(fleets => a.response(fleets))
      case a @ Action.ReadPlanets(_, _) =>
        ogameDriver
          .readPlanets()
          .map(planets => a.response(planets))
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
    val action = Action.GetAirFleet(clock.now())
    exec(action)
  }

  override def readPlanets(): Task[List[PlayerPlanet]] = {
    val action = Action.ReadPlanets(clock.now())
    exec(action)
  }

  override def sendFleet(req: SendFleetRequest): Task[ZonedDateTime] = {
    val action = Action.SendFleet(clock.now(), req)
    exec(action)
  }

  override def getFleetOnPlanet(planet: PlayerPlanet): Task[PlanetFleet] = {
    val action = Action.RefreshFleetOnPlanetStatus(clock.now(), planet)
    exec(action)
  }

  override def readSupplyPage(playerPlanet: PlayerPlanet): Task[SuppliesPageData] = {
    val action = Action.ReadSupplyPage(clock.now(), playerPlanet.id)
    exec(action)
  }

  override def buildSupplyBuilding(
      suppliesBuilding: SuppliesBuilding,
      level: Int Refined Positive,
      planet: PlayerPlanet
  ): Task[ZonedDateTime] = {
    val action = Action.BuildSupply(suppliesBuilding, level, clock.now(), planet.id)
    exec(action)
  }

  private def exec[T](action: Action[T]) = {
    Task.parMap2(
      queue.offer(action),
      subject
        .collect {
          case r if r.uuid == action.uuid =>
            val value = action.defer(r.value)
            logger.debug(s"action response: ${pprint.apply(value)}")
            value
        }
        .consumeWith(Consumer.head)
    )((_, result) => result)
  }
}
