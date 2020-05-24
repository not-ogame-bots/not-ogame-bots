package not.ogame.bots.ghostbuster.processors

import java.time.Clock

import com.softwaremill.quicklens._
import not.ogame.bots.ShipType
import not.ogame.bots.ghostbuster._

//class BuildMtUpToCapacityProcessor(botConfig: BotConfig, jitterProvider: RandomTimeJitter)(implicit clock: Clock) {
//  def apply(state: State.LoggedIn): State.LoggedIn = {
//    if (botConfig.buildMtUpToCapacity) {
//      state
//        .modify(_.scheduledTasks)
//        .setTo(state.scheduledTasks ++ createTasks(state))
//    } else {
//      state
//    }
//  }
//
//  private def createTasks(state: State.LoggedIn) = {
//    state.planets.flatMap(ps => processSinglePlanet(ps, state.scheduledTasks))
//  }
//
//  private def processSinglePlanet(planetState: PlanetState, tasks: List[Action]): List[Action] = {
//    if (planetState.isIdle && noBuildingsInQueue(tasks, planetState.id)) {
//      planetState.fleetOnPlanet
//        .get(ShipType.SmallCargoShip)
//        .map { shipAmount =>
//          tryBuildingMt(planetState, shipAmount)
//        }
//        .getOrElse(
//          List(Action.RefreshFleetOnPlanetStatus(clock.instant(), planetState.id))
//        ) //TODO shipInProgress should have richer information
//    } else if (planetState.shipInProgress) {
//      refreshShipIfNecessary(planetState, tasks) ++ refreshBuildQueueIfNecessary(planetState, tasks)
//    } else {
//      List.empty
//    }
//  }
//
//  private def refreshShipIfNecessary(planetState: PlanetState, tasks: List[Action]): List[Action] = {
//    if (!refreshShipScheduled(tasks, planetState.id, ShipType.SmallCargoShip)) {
//      List(
//        Action.RefreshFleetOnPlanetStatus(
//          planetState.suppliesPage.currentShipyardProgress.get.finishTimestamp,
//          planetState.id
//        )
//      )
//    } else {
//      List.empty
//    }
//  }
//
//  private def refreshBuildQueueIfNecessary(planetState: PlanetState, tasks: List[Action]): List[Action] = {
//    if (!refreshScheduled(tasks, planetState.id)) {
//      List(
//        Action.RefreshSupplyAndFacilityPage(
//          planetState.suppliesPage.currentShipyardProgress.get.finishTimestamp,
//          planetState.id
//        )
//      )
//    } else {
//      List.empty
//    }
//  }
//
//  private def tryBuildingMt(
//      planetState: PlanetState,
//      shipAmount: Int
//  ): List[Action] = {
//    val capacity = planetState.suppliesPage.currentCapacity
//    val expectedAmount = capacity.metal / 5000 + capacity.deuterium / 5000 + capacity.crystal / 5000
//    if (expectedAmount > shipAmount) {
//      buildShip(planetState, ShipType.SmallCargoShip, jitterProvider, expectedAmount - shipAmount).toList
//    } else {
//      List.empty
//    }
//  }
//}
