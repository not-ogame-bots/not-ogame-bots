package not.ogame.bots.ghost

import java.time.ZonedDateTime

import cats.effect.concurrent.MVar
import com.typesafe.scalalogging.StrictLogging
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.CanBlock
import not.ogame.bots.{
  Coordinates,
  FacilitiesBuildingLevels,
  FacilityBuilding,
  FacilityPageData,
  Fleet,
  FleetId,
  MyFleet,
  OgameDriver,
  PlanetId,
  PlayerPlanet,
  RealLocalClock,
  Resources,
  SendFleetRequest,
  ShipType,
  SuppliesBuilding,
  SuppliesBuildingLevels,
  SuppliesPageData
}
import not.ogame.bots.ghostbuster.executor.{Action, EmptyStateChangeListener, Response, TaskExecutorImpl}
import monix.execution.Scheduler.Implicits.global
class TaskExecutorConcurrentTest extends munit.FunSuite with StrictLogging {
  (0 to 100).foreach { i =>
    test(s"Asdasd $i") {
      val executor = new TaskExecutorImpl(new OgameDriver[Task] {
        override def login(): Task[Unit] = Task.unit

        override def readSuppliesPage(planetId: String): Task[SuppliesPageData] = Task.eval(
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

        override def buildSuppliesBuilding(planetId: String, suppliesBuilding: SuppliesBuilding): Task[Unit] = Task.eval(())

        override def readFacilityPage(planetId: String): Task[FacilityPageData] = Task.eval(
          FacilityPageData(ZonedDateTime.now(), Resources.Zero, Resources.Zero, Resources.Zero, FacilitiesBuildingLevels(Map.empty), None)
        )

        override def buildFacilityBuilding(planetId: String, facilityBuilding: FacilityBuilding): Task[Unit] =
          Task.eval(())

        override def buildShips(planetId: String, shipType: ShipType, count: Int): Task[Unit] = {
          Task.eval(())
        }

        override def checkFleetOnPlanet(planetId: String): Task[Map[ShipType, Int]] = Task.eval(Map.empty)

        override def readAllFleets(): Task[List[Fleet]] = Task.eval(List.empty)

        override def readMyFleets(): Task[List[MyFleet]] = Task.eval(List.empty)

        override def sendFleet(sendFleetRequest: SendFleetRequest): Task[Unit] = Task.eval(())

        override def returnFleet(fleetId: FleetId): Task[Unit] = Task.eval(())

        override def readPlanets(): Task[List[PlayerPlanet]] = Task.eval(List.empty)

        override def checkIsLoggedIn(): Task[Boolean] = Task.eval(true)
      }, new RealLocalClock(), new EmptyStateChangeListener[Task])

      executor.run().runToFuture
      Task
        .parSequence(
          List(
            executor.readAllFleets(),
            executor.readMyFleets().onErrorRestartIf { _ =>
              logger.info("restert my fleet")
              true
            },
            executor.readPlanets(),
            executor.buildShip(ShipType.SmallCargoShip, 1, PlayerPlanet(PlanetId("a"), Coordinates(1, 2, 3))).onErrorRestartIf { _ =>
              logger.info("restert buildShip")
              true
            }
          )
        )
        .runSyncUnsafe()
    }
  }
}
