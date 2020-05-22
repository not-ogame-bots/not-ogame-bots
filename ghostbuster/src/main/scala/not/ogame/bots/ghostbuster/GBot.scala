package not.ogame.bots.ghostbuster

import java.time.Clock

import com.softwaremill.quicklens._
import eu.timepit.refined.numeric.Positive
import not.ogame.bots.facts.{FacilityBuildingCosts, SuppliesBuildingCosts}
import not.ogame.bots.selenium._
import not.ogame.bots.{BuildingProgress, FacilitiesBuildingLevels, FacilityBuilding, Resources, SuppliesBuilding, SuppliesPageData}

class GBot(jitterProvider: RandomTimeJitter, botConfig: BotConfig)(implicit clock: Clock) {
  def nextStep(state: PlanetState.LoggedIn): PlanetState.LoggedIn = {
    println(s"processing next state $state")
    val nextState = state match {
      case loggedState @ PlanetState.LoggedIn(SuppliesPageData(_, _, _, _, _, Some(buildingProgress)), tasks, _) =>
        scheduleRefreshAfterBuildingFinishes(loggedState, buildingProgress, tasks)
      case loggedState @ PlanetState.LoggedIn(suppliesPage, tasks, facilityBuildingLevels) if !isBuildingInQueue(tasks) =>
        handleWishWhenLogged(suppliesPage, facilityBuildingLevels)
          .map(task => loggedState.modify(_.scheduledTasks).setTo(loggedState.scheduledTasks :+ task))
          .getOrElse(loggedState)
      case other => other
    }
    println(s"calculated next state: $nextState")
    nextState
  }

  private def isFacilityBuildingInQueue(tasks: List[Task]): Boolean = {
    tasks
      .collectFirst {
        case Task.BuildFacility(_, _, _) => true
      }
      .getOrElse(false)
  }

  private def isSupplyBuildingInQueue(tasks: List[Task]): Boolean = {
    tasks
      .collectFirst {
        case Task.BuildSupply(_, _, _) => true
      }
      .getOrElse(false)
  }

  private def isBuildingInQueue(tasks: List[Task]): Boolean = {
    isSupplyBuildingInQueue(tasks) || isFacilityBuildingInQueue(tasks)
  }

  private def scheduleRefreshAfterBuildingFinishes(
      loggedState: PlanetState.LoggedIn,
      buildingProgress: BuildingProgress,
      tasks: List[Task]
  ) = {
    tasks.find(_.isInstanceOf[Task.RefreshSupplyAndFacilityPage]) match {
      case None =>
        loggedState
          .modify(_.scheduledTasks)
          .setTo(loggedState.scheduledTasks :+ Task.refreshSupplyPage(buildingProgress.finishTimestamp))
      case _ => loggedState
    }
  }

  private def handleWishWhenLogged(suppliesPage: SuppliesPageData, facilityBuildingLevels: FacilitiesBuildingLevels) = {
    botConfig.wishlist.collectFirst {
      case w: Wish.BuildSupply if suppliesPage.suppliesLevels.map(w.suppliesBuilding).value < w.level.value => buildSupply(suppliesPage, w)
      case w: Wish.BuildFacility if facilityBuildingLevels.map(w.facility).value < w.level.value =>
        buildFacility(suppliesPage, facilityBuildingLevels, w)
    }.flatten
  }

  private def buildSupply(suppliesPage: SuppliesPageData, w: Wish.BuildSupply) = {
    if (suppliesPage.currentResources.energy < 0) {
      buildBuilding(
        suppliesPage,
        Wish.BuildSupply(
          SuppliesBuilding.SolarPlant,
          nextLevel(suppliesPage, SuppliesBuilding.MetalStorage)
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
    if (suppliesPage.currentResources.gtEqTo(requiredResources)) {
      Some(Task.BuildSupply(buildWish.suppliesBuilding, nextLevel(suppliesPage, buildWish.suppliesBuilding), clock.instant()))
    } else {
      if (suppliesPage.currentCapacity.gtEqTo(requiredResources)) {
        val stillNeed = requiredResources.difference(suppliesPage.currentResources)
        val hoursToWait = stillNeed.div(suppliesPage.currentProduction).max
        val secondsToWait = (hoursToWait * 3600).toInt + jitterProvider.getJitterInSeconds()
        val timeOfExecution = clock.instant().plusSeconds(secondsToWait)
        Some(Task.BuildSupply(buildWish.suppliesBuilding, nextLevel(suppliesPage, buildWish.suppliesBuilding), timeOfExecution))
      } else {
        buildStorage(suppliesPage, requiredResources)
      }
    }
  }

  private def buildFacility(
      suppliesPage: SuppliesPageData,
      facilitiesBuildingLevels: FacilitiesBuildingLevels,
      buildWish: Wish.BuildFacility
  ): Option[Task] = {
    val requiredResources =
      FacilityBuildingCosts.buildingCost(buildWish.facility, nextLevel(facilitiesBuildingLevels, buildWish.facility))
    if (suppliesPage.currentResources.gtEqTo(requiredResources)) {
      Some(Task.BuildFacility(buildWish.facility, nextLevel(facilitiesBuildingLevels, buildWish.facility), clock.instant()))
    } else {
      if (suppliesPage.currentCapacity.gtEqTo(requiredResources)) {
        val stillNeed = requiredResources.difference(suppliesPage.currentResources)
        val hoursToWait = stillNeed.div(suppliesPage.currentProduction).max
        val secondsToWait = (hoursToWait * 3600).toInt + jitterProvider.getJitterInSeconds()
        val timeOfExecution = clock.instant().plusSeconds(secondsToWait)
        Some(Task.BuildFacility(buildWish.facility, nextLevel(facilitiesBuildingLevels, buildWish.facility), timeOfExecution))
      } else {
        buildStorage(suppliesPage, requiredResources)
      }
    }
  }

  private def buildStorage(
      suppliesPage: SuppliesPageData,
      requiredResources: Resources
  ): Option[Task.BuildSupply] = {
    requiredResources.difference(suppliesPage.currentCapacity) match {
      case Resources(m, _, _, _) if m > 0 =>
        buildBuilding(
          suppliesPage,
          Wish.BuildSupply(
            SuppliesBuilding.MetalStorage,
            nextLevel(suppliesPage, SuppliesBuilding.MetalStorage)
          )
        )
      case Resources(_, c, _, _) if c > 0 =>
        buildBuilding(
          suppliesPage,
          Wish.BuildSupply(
            SuppliesBuilding.CrystalStorage,
            nextLevel(suppliesPage, SuppliesBuilding.CrystalStorage)
          )
        )
      case Resources(_, _, d, _) if d > 0 =>
        buildBuilding(
          suppliesPage,
          Wish.BuildSupply(
            SuppliesBuilding.DeuteriumStorage,
            nextLevel(suppliesPage, SuppliesBuilding.DeuteriumStorage)
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
