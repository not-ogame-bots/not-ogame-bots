package not.ogame.bots.ghostbuster

import java.time.Clock

import cats.MonadError
import cats.effect.Timer
import cats.implicits._
import com.softwaremill.quicklens._
import not.ogame.bots.ghostbuster.PlanetState.LoggedIn
import not.ogame.bots.ghostbuster.TaskExecutor._
import not.ogame.bots.{OgameDriver, ShipType, SuppliesBuilding}

class TaskExecutor[F[_]: MonError: Timer](ogameDriver: OgameDriver[F], gBot: GBot)(implicit clock: Clock) {
  def execute(state: PlanetState): F[PlanetState.LoggedIn] = {
    state match {
      case loggedOut: PlanetState.LoggedOut =>
        logIn(loggedOut)
          .map(gBot.nextStep)
      case loggedIn: PlanetState.LoggedIn =>
        executeAl(loggedIn.scheduledTasks, loggedIn).map(s => s: PlanetState.LoggedIn)
    }
  }

  private def logIn(state: PlanetState): F[PlanetState.LoggedIn] = {
    (for {
      _ <- ogameDriver.login()
      sp <- ogameDriver.readSuppliesPage(PlanetId)
      fp <- ogameDriver.readFacilityBuildingsLevels(PlanetId)
    } yield PlanetState.LoggedIn(sp, state.scheduledTasks, fp, Map.empty))
      .handleErrorWith { e =>
        e.printStackTrace()
        logIn(state)
      }
  }

  private def executeAl(tasks: List[Task], state: LoggedIn): F[PlanetState.LoggedIn] = {
    tasks match {
      case ::(head, next) if clock.instant().isAfter(head.executeAfter) =>
        execute(head, state)
          .handleErrorWith { e =>
            e.printStackTrace()
            logIn(state)
          }
          .flatMap { s =>
            executeAl(
              next,
              gBot.nextStep(
                s.modify(_.scheduledTasks)
                  .setTo(next)
              )
            )
          }
      case ::(_, next) => executeAl(next, state)
      case Nil         => state.pure[F]
    }
  }

  private def execute(task: Task, state: LoggedIn): F[LoggedIn] = {
    task match {
      case Task.BuildSupply(suppliesBuilding, _, _) =>
        buildSupplyBuilding(state, suppliesBuilding)
      case Task.RefreshSupplyAndFacilityPage(_) =>
        for {
          sp <- ogameDriver.readSuppliesPage(PlanetId)
          fp <- ogameDriver.readFacilityBuildingsLevels(PlanetId)
        } yield state
          .modify(_.suppliesPage)
          .setTo(sp)
          .modify(_.facilityBuildingLevels)
          .setTo(fp)
      case Task.BuildFacility(facilityBuilding, _, _) =>
        for {
          _ <- ogameDriver.buildFacilityBuilding(PlanetId, facilityBuilding)
          newFacilityLevels <- ogameDriver.readFacilityBuildingsLevels(PlanetId)
        } yield state.modify(_.facilityBuildingLevels).setTo(newFacilityLevels)

      case Task.RefreshFleetOnPlanetStatus(shipType, _) =>
        ogameDriver
          .checkFleetOnPlanet(PlanetId, shipType)
          .map(amount => state.modify(_.fleetOnPlanet).setTo(state.fleetOnPlanet ++ Map(shipType -> amount)))
      case Task.BuildShip(amount, shipType, _) =>
        ogameDriver.buildShips(PlanetId, shipType, amount) >> ogameDriver
          .readSuppliesPage(PlanetId)
          .map(suppliesPage => state.modify(_.suppliesPage).setTo(suppliesPage))
      case Task.DumpActivity(_) =>
        ogameDriver.checkFleetOnPlanet(PlanetId, ShipType.SmallCargoShip) >> ogameDriver.readSuppliesPage(PlanetId).map(_ => state)
    }
  }

  private def buildSupplyBuilding(state: LoggedIn, suppliesBuilding: SuppliesBuilding) = {
    //TODO check if level is correct
    //TODO move executeAfter outside of task?
    //TODO check resources
    for {
      _ <- ogameDriver.buildSuppliesBuilding(PlanetId, suppliesBuilding)
      newSuppliesPage <- ogameDriver.readSuppliesPage(PlanetId)
    } yield {
      state
        .modify(_.suppliesPage)
        .setTo(newSuppliesPage)
    }
  }
}

object TaskExecutor {
  type MonError[F[_]] = MonadError[F, Throwable]
  val PlanetId = "33653280"
}
