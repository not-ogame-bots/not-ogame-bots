package not.ogame.bots.ghost

import java.time.ZonedDateTime

import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.execution.atomic.AtomicInt
import monix.reactive.Observable
import not.ogame.bots._
import not.ogame.bots.ghostbuster.executor.impl.TaskExecutorImpl
import not.ogame.bots.ghostbuster.notifications.{Notification, NotificationAware}

class TaskExecutorConcurrentTest extends munit.FunSuite with StrictLogging {
  (0 to 100).foreach { i =>
    test(s"Asdasd $i") {
      val executor = new TaskExecutorImpl(new OgameDriver[Task] with NotificationAware {
        override def login(): Task[Unit] = Task.unit
        override def readSuppliesPage(planetId: PlanetId): Task[SuppliesPageData] = Task.eval(
          SuppliesPageData(
            ZonedDateTime.now(),
            Resources.Zero,
            Resources.Zero,
            Resources.Zero,
            SuppliesBuildingLevels(Map.empty),
            None,
            None
          )
        )
        override def buildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding): Task[Unit] = Task.eval(())
        override def readFacilityPage(planetId: PlanetId): Task[FacilityPageData] = Task.eval(
          FacilityPageData(ZonedDateTime.now(), Resources.Zero, Resources.Zero, Resources.Zero, FacilitiesBuildingLevels(Map.empty), None)
        )
        override def buildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding): Task[Unit] =
          Task.eval(())
        val counter = AtomicInt(0)
        override def buildShips(planetId: PlanetId, shipType: ShipType, count: Int): Task[Unit] = {
          Task.eval(println(s"counter = ${counter.get()}")) >>
            (if (counter.get() == 0) {
               Task.eval(counter.increment()) >>
                 Task.raiseError(new RuntimeException("asd"))
             } else {
               Task.eval(())
             })
        }

        override def readAllFleets(): Task[List[Fleet]] = Task.eval(List.empty)
        override def readMyFleets(): Task[MyFleetPageData] = Task.eval(MyFleetPageData(List.empty, MyFleetSlots(0, 0, 0, 0)))
        override def sendFleet(sendFleetRequest: SendFleetRequest): Task[Unit] = Task.eval(())
        override def returnFleet(fleetId: FleetId): Task[Unit] = Task.eval(())
        override def readPlanets(): Task[List[PlayerPlanet]] = Task.eval(List.empty)
        override def checkLoginStatus(): Task[Boolean] = Task.eval(true)
        override def readFleetPage(planetId: PlanetId): Task[FleetPageData] =
          Task.eval(
            FleetPageData(ZonedDateTime.now(), Resources.Zero, Resources.Zero, Resources.Zero, FleetSlots(0, 0, 0, 0, 0, 0), Map.empty)
          )
        override def subscribeToNotifications: Observable[Notification] = ???
        override def readMyOffers(): Task[List[MyOffer]] = ???
        override def createOffer(planetId: PlanetId, newOffer: MyOffer): Task[Unit] = ???
        override def readTechnologyPage(planetId: PlanetId): Task[TechnologyPageData] = ???
        override def startResearch(planetId: PlanetId, technology: Technology): Task[Unit] = ???
        override def buildSolarSatellites(planetId: PlanetId, count: Int): Task[Unit] = ???
      })(new RealLocalClock())

      executor.run().runToFuture
//      Task
//        .parSequence(
//          List(
//            executor.readAllFleets(),
//            executor.readMyFleets().onErrorRestartIf { _ =>
//              logger.info("restert my fleet")
//              true
//            },
//            executor.readPlanets(),
//            executor.buildShip(ShipType.SmallCargoShip, 1, PlayerPlanet(PlanetId("a"), Coordinates(1, 2, 3))).onErrorRestartIf { _ =>
//              logger.info("restert buildShip")
//              true
//            }
//          )
//        )
//        .runSyncUnsafe()
    }
  }
}
