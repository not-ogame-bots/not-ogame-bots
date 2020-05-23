package not.ogame.bots.ghostbuster.processors

import java.time.Clock

import com.softwaremill.quicklens._
import not.ogame.bots.{Resources, ShipType}
import not.ogame.bots.facts.ShipCosts
import not.ogame.bots.ghostbuster._

class BuildMtUpToCapacityProcessor(botConfig: BotConfig, jitterProvider: RandomTimeJitter)(implicit clock: Clock) {
  def apply(state: State.LoggedIn): State.LoggedIn = {
    if (botConfig.buildMtUpToCapacity) {
      state
        .modify(_.scheduledTasks)
        .setTo(state.scheduledTasks ++ createTasks(state))
    } else {
      state
    }
  }

  private def createTasks(state: State.LoggedIn) = {
    state.planets.flatMap(ps => processSinglePlanet(ps, state.scheduledTasks))
  }

  private def processSinglePlanet(planetState: PlanetState, tasks: List[Task]): List[Task] = {
    if (planetState.isIdle && noBuildingsInQueue(tasks, planetState.id)) {
      planetState.fleetOnPlanet
        .get(ShipType.SmallCargoShip)
        .map { shipAmount =>
          tryBuildingMt(planetState, shipAmount)
        }
        .getOrElse(
          List(Task.RefreshFleetOnPlanetStatus(ShipType.SmallCargoShip, clock.instant(), planetState.id))
        ) //TODO add next case with refresh after building one ship, also shipInProgress should have richer information
    } else if (planetState.shipInProgress) {
      refreshShipIfNecessary(planetState, tasks) ++ refreshBuildQueueIfNecessary(planetState, tasks)
    } else {
      List.empty
    }
  }

  private def refreshShipIfNecessary(planetState: PlanetState, tasks: List[Task]): List[Task] = {
    if (!refreshShipScheduled(tasks, planetState.id, ShipType.SmallCargoShip)) {
      List(
        Task.RefreshFleetOnPlanetStatus(
          ShipType.SmallCargoShip,
          planetState.suppliesPage.currentShipyardProgress.get.finishTimestamp,
          planetState.id
        )
      )
    } else {
      List.empty
    }
  }

  private def refreshBuildQueueIfNecessary(planetState: PlanetState, tasks: List[Task]): List[Task] = {
    if (!refreshScheduled(tasks, planetState.id)) {
      List(
        Task.RefreshSupplyAndFacilityPage(
          planetState.suppliesPage.currentShipyardProgress.get.finishTimestamp,
          planetState.id
        )
      )
    } else {
      List.empty
    }
  }

  private def tryBuildingMt(
      planetState: PlanetState,
      shipAmount: Int
  ): List[Task] = {
    val capacity = planetState.suppliesPage.currentCapacity
    val expectedAmount = capacity.metal / 5000 + capacity.deuterium / 5000 + capacity.crystal / 5000
    if (expectedAmount > shipAmount) {
      List(buildShip(planetState, ShipType.SmallCargoShip, jitterProvider, expectedAmount - shipAmount))
    } else {
      List.empty
    }
  }
}
