package not.ogame.bots.ghostbuster.processors

import java.time.Clock

import eu.timepit.refined.numeric.Positive
import not.ogame.bots.{FacilitiesBuildingLevels, FacilityBuilding, Resources, SuppliesBuilding, SuppliesPageData}
import not.ogame.bots.facts.{FacilityBuildingCosts, SuppliesBuildingCosts}
import not.ogame.bots.ghostbuster.{BotConfig, PlanetState, RandomTimeJitter, State, Task, Wish}
import not.ogame.bots.selenium.refineVUnsafe
import com.softwaremill.quicklens._

class WishlistProcessor(botConfig: BotConfig, jitterProvider: RandomTimeJitter)(implicit clock: Clock) {
  def apply(state: State.LoggedIn): State.LoggedIn = {
    if (botConfig.useWishlist) {
      state.modify(_.scheduledTasks).setTo(state.scheduledTasks ++ state.planets.flatMap(ps => process(ps, state.scheduledTasks)))
    } else {
      state
    }
  }

  private def process(planetState: PlanetState, tasks: List[Task]): List[Task] = {
    if (planetState.isIdle && !buildingScheduled(tasks, planetState.id)) {
      scheduleNextWish(planetState).toList
    } else if (planetState.buildingInProgress && !refreshScheduled(tasks, planetState.id)) {
      List(Task.RefreshSupplyAndFacilityPage(planetState.suppliesPage.currentBuildingProgress.get.finishTimestamp, planetState.id))
    } else {
      List.empty
    }
  }

  private def scheduleNextWish(planetState: PlanetState) = {
    val suppliesPage = planetState.suppliesPage
    val facilityBuildingLevels = planetState.facilityBuildingLevels
    botConfig.wishlist.collectFirst {
      case w: Wish.BuildSupply
          if suppliesPage.suppliesLevels.map(w.suppliesBuilding).value < w.level.value && w.planetId == planetState.id =>
        buildSupply(suppliesPage, w)
      case w: Wish.BuildFacility if facilityBuildingLevels.map(w.facility).value < w.level.value && w.planetId == planetState.id =>
        buildFacility(suppliesPage, facilityBuildingLevels, w, planetState.id)
      case w: Wish.BuildShip if !planetState.fleetOnPlanet.contains(w.shipType) =>
        Some(Task.RefreshFleetOnPlanetStatus(w.shipType, clock.instant(), planetState.id))
      case w: Wish.BuildShip if planetState.fleetOnPlanet.contains(w.shipType) =>
        val currentAmount = planetState.fleetOnPlanet(w.shipType)
        if (currentAmount < w.amount.value) {
          buildShip(planetState, w.shipType, jitterProvider, w.amount.value - currentAmount, botConfig.allowWaiting)
        } else {
          None
        }
    }.flatten
  }

  private def buildSupply(suppliesPage: SuppliesPageData, w: Wish.BuildSupply) = {
    if (suppliesPage.currentResources.energy < 0) {
      buildBuilding(
        suppliesPage,
        Wish.BuildSupply(
          SuppliesBuilding.SolarPlant,
          nextLevel(suppliesPage, SuppliesBuilding.MetalStorage),
          w.planetId
        )
      )
    } else {
      buildBuilding(suppliesPage, w)
    }
  }

  private def buildBuilding(
      suppliesPage: SuppliesPageData,
      buildWish: Wish.BuildSupply
  ): Option[Task.BuildSupply] = {
    val requiredResources =
      SuppliesBuildingCosts.buildingCost(buildWish.suppliesBuilding, nextLevel(suppliesPage, buildWish.suppliesBuilding))
    if (suppliesPage.currentResources.gtEqTo(requiredResources)) { //TODO can be simplified
      Some(
        Task
          .BuildSupply(buildWish.suppliesBuilding, nextLevel(suppliesPage, buildWish.suppliesBuilding), clock.instant(), buildWish.planetId)
      )
    } else if (botConfig.allowWaiting) {
      if (suppliesPage.currentCapacity.gtEqTo(requiredResources)) {
        val stillNeed = requiredResources.difference(suppliesPage.currentResources)
        val hoursToWait = stillNeed.div(suppliesPage.currentProduction).max
        val secondsToWait = (hoursToWait * 3600).toInt + jitterProvider.getJitterInSeconds()
        val timeOfExecution = clock.instant().plusSeconds(secondsToWait)
        Some(
          Task.BuildSupply(
            buildWish.suppliesBuilding,
            nextLevel(suppliesPage, buildWish.suppliesBuilding),
            timeOfExecution,
            buildWish.planetId
          )
        )
      } else {
        buildStorage(suppliesPage, requiredResources, buildWish.planetId)
      }
    } else {
      None
    }
  }

  private def buildFacility(
      suppliesPage: SuppliesPageData,
      facilitiesBuildingLevels: FacilitiesBuildingLevels,
      buildWish: Wish.BuildFacility,
      planetId: String
  ): Option[Task] = {
    val requiredResources =
      FacilityBuildingCosts.buildingCost(buildWish.facility, nextLevel(facilitiesBuildingLevels, buildWish.facility))
    if (suppliesPage.currentResources.gtEqTo(requiredResources)) {
      Some(Task.BuildFacility(buildWish.facility, nextLevel(facilitiesBuildingLevels, buildWish.facility), clock.instant(), planetId))
    } else if (botConfig.allowWaiting) {
      if (suppliesPage.currentCapacity.gtEqTo(requiredResources)) {
        val stillNeed = requiredResources.difference(suppliesPage.currentResources)
        val hoursToWait = stillNeed.div(suppliesPage.currentProduction).max
        val secondsToWait = (hoursToWait * 3600).toInt + jitterProvider.getJitterInSeconds()
        val timeOfExecution = clock.instant().plusSeconds(secondsToWait)
        Some(Task.BuildFacility(buildWish.facility, nextLevel(facilitiesBuildingLevels, buildWish.facility), timeOfExecution, planetId))
      } else {
        buildStorage(suppliesPage, requiredResources, planetId)
      }
    } else {
      None
    }
  }

  private def buildStorage(
      suppliesPage: SuppliesPageData,
      requiredResources: Resources,
      planetId: String
  ): Option[Task.BuildSupply] = {
    requiredResources.difference(suppliesPage.currentCapacity) match {
      case Resources(m, _, _, _) if m > 0 =>
        buildBuilding(
          suppliesPage,
          Wish.BuildSupply(
            SuppliesBuilding.MetalStorage,
            nextLevel(suppliesPage, SuppliesBuilding.MetalStorage),
            planetId
          )
        )
      case Resources(_, c, _, _) if c > 0 =>
        buildBuilding(
          suppliesPage,
          Wish.BuildSupply(
            SuppliesBuilding.CrystalStorage,
            nextLevel(suppliesPage, SuppliesBuilding.CrystalStorage),
            planetId
          )
        )
      case Resources(_, _, d, _) if d > 0 =>
        buildBuilding(
          suppliesPage,
          Wish.BuildSupply(
            SuppliesBuilding.DeuteriumStorage,
            nextLevel(suppliesPage, SuppliesBuilding.DeuteriumStorage),
            planetId
          )
        )
    }
  }

  private def nextLevel(suppliesPage: SuppliesPageData, building: SuppliesBuilding) = {
    refineVUnsafe[Positive, Int](suppliesPage.suppliesLevels.map(building).value + 1)
  }

  private def nextLevel(facilitiesBuildingLevels: FacilitiesBuildingLevels, building: FacilityBuilding) = {
    refineVUnsafe[Positive, Int](facilitiesBuildingLevels.map(building).value + 1)
  }
}
