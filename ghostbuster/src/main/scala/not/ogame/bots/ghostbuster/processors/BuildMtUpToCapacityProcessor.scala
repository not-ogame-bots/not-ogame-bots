package not.ogame.bots.ghostbuster.processors

import java.time.Clock

import com.softwaremill.quicklens._
import not.ogame.bots.facts.ShipCosts
import not.ogame.bots.ghostbuster.{BotConfig, PlanetState, RandomTimeJitter, Task}
import not.ogame.bots.{ShipType, SuppliesPageData}

class BuildMtUpToCapacityProcessor(botConfig: BotConfig, jitterProvider: RandomTimeJitter)(implicit clock: Clock) {
  def apply(state: PlanetState.LoggedIn): PlanetState.LoggedIn = {
    if (botConfig.buildMtUpToCapacity) {
      fleetBuildingProcessor(state)
    } else {
      state
    }
  }

  private def fleetBuildingProcessor(state: PlanetState.LoggedIn): PlanetState.LoggedIn = {
    state match {
      case PlanetState.LoggedIn(SuppliesPageData(_, _, _, capacity, _, None, None), tasks, _, fleetOnPlanet) if !isBuildingInQueue(tasks) =>
        fleetOnPlanet
          .get(ShipType.SmallCargoShip)
          .map { shipAmount =>
            tryBuildingMt(state, shipAmount)
          }
          .getOrElse(
            state
              .modify(_.scheduledTasks)
              .setTo(state.scheduledTasks :+ Task.refreshFleetOnPlanetStatus(ShipType.SmallCargoShip, clock.instant()))
          ) //TODO add next case with refresh after building one ship, also shipInProgress should have richer information
      case other => other
    }
  }

  private def tryBuildingMt(
      state: PlanetState.LoggedIn,
      shipAmount: Int
  ) = {
    val capacity = state.suppliesPage.currentCapacity
    val currentResources = state.suppliesPage.currentResources
    val currentProduction = state.suppliesPage.currentProduction
    val expectedAmount = capacity.metal / 5000 + capacity.deuterium / 5000 + capacity.crystal / 5000
    if (expectedAmount > shipAmount) {
      val requiredResources = ShipCosts.shipCost(ShipType.SmallCargoShip)
      val canBuildAmount = currentResources.div(requiredResources).map(_.toInt).min
      if (canBuildAmount > 1) {
        state
          .modify(_.scheduledTasks)
          .setTo(state.scheduledTasks :+ Task.BuildShip(canBuildAmount, ShipType.SmallCargoShip, clock.instant()))
      } else {
        val stillNeed = requiredResources.difference(currentResources)
        val hoursToWait = stillNeed.div(currentProduction).max
        val secondsToWait = (hoursToWait * 3600).toInt + jitterProvider.getJitterInSeconds()
        val timeOfExecution = clock.instant().plusSeconds(secondsToWait)
        state
          .modify(_.scheduledTasks)
          .setTo(state.scheduledTasks :+ Task.BuildShip(canBuildAmount, ShipType.SmallCargoShip, timeOfExecution))
      }
    } else {
      state
    }
  }
}
