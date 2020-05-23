package not.ogame.bots.ghostbuster.two

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit

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
import not.ogame.bots.facts.SuppliesBuildingCosts
import not.ogame.bots.ghostbuster.{Action, PlanetFleet, Response, Wish}
import not.ogame.bots.selenium.refineVUnsafe

import scala.concurrent.duration._

class FlyAndBuildProcessor(taskExecutor: TaskExecutor, clock: Clock, wishList: List[Wish]) {
  println(s"wishList ${wishList}")
  private var planetSendingCount = 0

  def run(): Task[Unit] = {
    for {
      planets <- taskExecutor.readPlanets()
      fleets <- taskExecutor.readAllFleets()
      _ <- fleets.find(
        f =>
          f.fleetAttitude == FleetAttitude.Friendly && f.fleetMissionType == FleetMissionType.Deployment && planets
            .exists(p => p.coordinates == f.to) && planets.exists(p => p.coordinates == f.from)
      ) match {
        case Some(fleet) =>
          val toPlanet = planets.find(p => fleet.to == p.coordinates).get
          val sleepTime = calculateSleepTime(fleet.arrivalTime)
          println(s"sleeping ~ ${sleepTime.toSeconds / 60} minutes")
          taskExecutor.waitSeconds(sleepTime) >> buildAndSend(toPlanet, planets)
        case None => lookAndSend(planets)
      }
    } yield ()
  }

  private def calculateSleepTime(futureTime: Instant) = {
    FiniteDuration(futureTime.toEpochMilli - clock.instant().toEpochMilli, TimeUnit.MILLISECONDS)
  }

  private def lookAndSend(planets: List[PlayerPlanet]): Task[Unit] = {
    val planetWithBiggestFleet = planets
      .map { planet =>
        taskExecutor.getFleetOnPlanet(planet)
      }
      .sequence
      .map(_.maxBy(_.fleet.size))

    planetWithBiggestFleet.flatMap { planet =>
      buildAndSend(planet.playerPlanet, planets)
    }
  }

  private def buildAndSend(currentPlanet: PlayerPlanet, planets: List[PlayerPlanet]): Task[Unit] = {
    val otherPlanets = planets.filterNot(p => p.id == currentPlanet.id)
    val targetPlanet = otherPlanets(planetSendingCount % otherPlanets.size)
    for {
      _ <- buildNextThingFromWishList(currentPlanet)
      _ <- sendFleet(from = currentPlanet, to = targetPlanet)
      _ <- buildAndSend(currentPlanet = targetPlanet, planets)
    } yield ()
  }

  private def sendFleet(from: PlayerPlanet, to: PlayerPlanet): Task[Instant] = {
    planetSendingCount = planetSendingCount + 1
    taskExecutor.sendFleet(
      SendFleetRequest(
        from.id,
        SendFleetRequestShips.AllShips,
        to.coordinates,
        FleetMissionType.Deployment,
        FleetResources.Max
      )
    )
  }

  private def buildNextThingFromWishList(planet: PlayerPlanet): Task[Unit] = {
    taskExecutor.readSupplyPage(planet).flatMap { suppliesPageData =>
      if (suppliesPageData.isIdle) {
        wishList
          .collectFirst {
            case w: Wish.BuildSupply
                if suppliesPageData.suppliesLevels.values(w.suppliesBuilding).value < w.level.value && w.planetId == planet.id =>
              buildSupplyBuilding(w, suppliesPageData, planet)
          }
          .sequence
          .void
      } else {
        Task.unit
      }
    }
  }

  private def buildSupplyBuilding(buildWish: Wish.BuildSupply, suppliesPageData: SuppliesPageData, planet: PlayerPlanet) = {
    val level = nextLevel(suppliesPageData, buildWish.suppliesBuilding)
    val requiredResources =
      SuppliesBuildingCosts.buildingCost(buildWish.suppliesBuilding, level)
    if (suppliesPageData.currentResources.gtEqTo(requiredResources)) { //TODO can be simplified
      taskExecutor.buildSupplyBuilding(buildWish.suppliesBuilding, level, planet).void
    } else {
      Task.unit
    }
  }

  private def nextLevel(suppliesPage: SuppliesPageData, building: SuppliesBuilding) = {
    refineVUnsafe[Positive, Int](suppliesPage.suppliesLevels.values(building).value + 1)
  }
}

trait TaskExecutor {
  def waitSeconds(duration: FiniteDuration): Task[Unit]

  def readAllFleets(): Task[List[Fleet]]

  def readPlanets(): Task[List[PlayerPlanet]]

  def sendFleet(req: SendFleetRequest): Task[Instant]

  def getFleetOnPlanet(planet: PlayerPlanet): Task[PlanetFleet]

  def readSupplyPage(playerPlanet: PlayerPlanet): Task[SuppliesPageData]

  def buildSupplyBuilding(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, planet: PlayerPlanet): Task[Instant]
}

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
      _ <- handleAction(action)
        .handleErrorWith { e =>
          e.printStackTrace()
          safeLogin >> handleAction(action)
        }
      _ <- processNextAction()
    } yield ()
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
      case a @ Action.DumpActivity(executionTime, planets, uuid) =>
        Task.unit //TODO
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
        }
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
