package not.ogame.bots.ghostbuster

import java.time.Clock

import cats.MonadError
import cats.effect.Timer
import cats.implicits._
import com.softwaremill.quicklens._
import not.ogame.bots.{OgameDriver, ShipType, SuppliesBuilding}
//
//class TaskExecutor[F[_]: MonError: Timer](ogameDriver: OgameDriver[F], gBot: GBot)(implicit clock: Clock) {
//  def execute(state: State): F[State.LoggedIn] = {
//    state match {
//      case loggedOut: State.LoggedOut =>
//        logIn(loggedOut)
//          .map(gBot.nextStep)
//      case loggedIn: State.LoggedIn =>
//        executeAl(loggedIn.scheduledTasks, loggedIn).map(s => s: State.LoggedIn)
//    }
//  }
//
//  private def logIn(state: State): F[State.LoggedIn] = {
//    (for {
//      _ <- ogameDriver.login()
//      planets <- ogameDriver.readPlanets()
//      planetStates <- planets.map { pp =>
//        for {
//          sp <- ogameDriver.readSuppliesPage(pp.id)
//          fp <- ogameDriver.readFacilityBuildingsLevels(pp.id)
//        } yield PlanetState(pp.id, pp.coordinates, sp, fp, Map.empty)
//      }.sequence
//      fleets <- ogameDriver.readAllFleets()
//    } yield State.LoggedIn(state.scheduledTasks, planetStates, fleets))
//      .handleErrorWith { e =>
//        e.printStackTrace()
//        logIn(state)
//      }
//  }
//
//  private def executeAl(tasks: List[Action], state: State.LoggedIn): F[State.LoggedIn] = {
//    tasks match {
//      case ::(head, next) if clock.instant().isAfter(head.executeAfter) =>
//        execute(head, state)
//          .handleErrorWith { e =>
//            e.printStackTrace()
//            logIn(state)
//          }
//          .flatMap { s =>
//            executeAl(
//              next,
//              gBot.nextStep(
//                s.modify(_.scheduledTasks)
//                  .setTo(next)
//              )
//            )
//          }
//      case ::(_, next) => executeAl(next, state)
//      case Nil         => state.pure[F]
//    }
//  }
//
//  private def execute(task: Action, state: LoggedIn): F[LoggedIn] = {
//    task match {
//      case Action.BuildSupply(suppliesBuilding, _, _, planetId) =>
//        buildSupplyBuilding(state, suppliesBuilding, planetId)
//      case Action.RefreshSupplyAndFacilityPage(_, planetId) =>
//        for {
//          sp <- ogameDriver.readSuppliesPage(planetId)
//          fp <- ogameDriver.readFacilityBuildingsLevels(planetId)
//        } yield {
//          val planetIdx = state.planets.indexWhere(_.id == planetId)
//          state
//            .modify(_.planets.at(planetIdx).suppliesPage)
//            .setTo(sp)
//            .modify(_.planets.at(planetIdx).facilityBuildingLevels)
//            .setTo(fp)
//        }
//      case Action.BuildFacility(facilityBuilding, _, _, planetId) =>
//        for {
//          _ <- ogameDriver.buildFacilityBuilding(planetId, facilityBuilding)
//          newFacilityLevels <- ogameDriver.readFacilityBuildingsLevels(planetId)
//        } yield {
//          val planetIdx = state.planets.indexWhere(_.id == planetId)
//          state
//            .modify(_.planets.at(planetIdx).facilityBuildingLevels)
//            .setTo(newFacilityLevels)
//        }
//      case Action.RefreshFleetOnPlanetStatus(_, planetId) =>
//        val planetIdx = state.planets.indexWhere(_.id == planetId)
//        ogameDriver
//          .checkFleetOnPlanet(planetId)
//          .map(
//            ships =>
//              state
//                .modify(_.planets.at(planetIdx).fleetOnPlanet)
//                .setTo(ships)
//          )
//      case Action.BuildShip(amount, shipType, _, planetId) =>
//        val planetIdx = state.planets.indexWhere(_.id == planetId)
//        ogameDriver.buildShips(planetId, shipType, amount) >> ogameDriver
//          .readSuppliesPage(planetId)
//          .map(suppliesPage => state.modify(_.planets.at(planetIdx).suppliesPage).setTo(suppliesPage))
//      case Action.DumpActivity(_, planets) =>
//        planets
//          .map { planet =>
//            ogameDriver.checkFleetOnPlanet(planet) >> ogameDriver.readSuppliesPage(planet)
//          }
//          .sequence
//          .map(_ => state)
//      case Action.SendFleet(_, sendFleetRequest) =>
//        ogameDriver.sendFleet(sendFleetRequest) >> ogameDriver.readAllFleets().map(fleets => state.modify(_.fleets).setTo(fleets))
//    }
//  }
//
//  private def buildSupplyBuilding(state: LoggedIn, suppliesBuilding: SuppliesBuilding, planetId: String) = {
//    //TODO check if level is correct
//    //TODO move executeAfter outside of task?
//    //TODO check resources
//    for {
//      _ <- ogameDriver.buildSuppliesBuilding(planetId, suppliesBuilding)
//      newSuppliesPage <- ogameDriver.readSuppliesPage(planetId)
//    } yield {
//      val planetIdx = state.planets.indexWhere(_.id == planetId)
//      modify(state)(_.planets.at(planetIdx))
//        .using(ps => ps.copy(suppliesPage = newSuppliesPage))
//    }
//  }
//}
//
//object TaskExecutor {
//  type MonError[F[_]] = MonadError[F, Throwable]
//}
