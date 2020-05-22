package not.ogame.bots.ghostbuster

import java.time.Clock

import com.softwaremill.quicklens._
import eu.timepit.refined.numeric.Positive
import not.ogame.bots._
import not.ogame.bots.facts.{FacilityBuildingCosts, ShipCosts, SuppliesBuildingCosts}
import not.ogame.bots.selenium._

import scala.reflect.ClassTag

class GBot(jitterProvider: RandomTimeJitter, botConfig: BotConfig)(implicit clock: Clock) {
  println(s"Creating with botConfig: $botConfig")

  def nextStep(state: PlanetState.LoggedIn): PlanetState.LoggedIn = {
    println(s"processing next state $state")
    val buildMtUpToCapacity = if (botConfig.buildMtUpToCapacity) {
      fleetBuildingProcessor _
    } else {
      identity[PlanetState.LoggedIn] _
    }
    val nextState = List(buildingProcessor(_), buildMtUpToCapacity).foldLeft(state)((acc, item) => item(acc))
    println(s"calculated next state: $nextState")
    nextState
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

  private def buildingProcessor(state: PlanetState.LoggedIn) = {
    state match {
      case loggedState @ PlanetState.LoggedIn(SuppliesPageData(_, _, _, _, _, Some(buildingProgress), _), tasks, _, _)
          if !checkAlreadyInQueue[Task.RefreshSupplyAndFacilityPage](tasks) =>
        loggedState
          .modify(_.scheduledTasks)
          .setTo(loggedState.scheduledTasks :+ Task.refreshSupplyPage(buildingProgress.finishTimestamp))
      case loggedState @ PlanetState.LoggedIn(suppliesPage, tasks, facilityBuildingLevels, _) if !isBuildingInQueue(tasks) =>
        handleWishWhenLogged(suppliesPage, facilityBuildingLevels)
          .map(task => loggedState.modify(_.scheduledTasks).setTo(loggedState.scheduledTasks :+ task))
          .getOrElse(loggedState)
      case other => other
    }
  }

  private def checkAlreadyInQueue[T: ClassTag](tasks: List[Task]): Boolean = {
    tasks.exists(t => implicitly[ClassTag[T]].runtimeClass.isInstance(t))
  }

  private def isBuildingInQueue(tasks: List[Task]): Boolean = {
    checkAlreadyInQueue[Task.BuildSupply](tasks) || checkAlreadyInQueue[Task.BuildFacility](tasks)
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
    if (suppliesPage.currentResources.gtEqTo(requiredResources)) { //TODO can be simplified
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
