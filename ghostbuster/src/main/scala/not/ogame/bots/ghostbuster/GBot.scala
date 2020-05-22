package not.ogame.bots.ghostbuster

import java.time.Clock

import com.softwaremill.quicklens._
import eu.timepit.refined.numeric.Positive
import not.ogame.bots.facts.SuppliesBuildingCosts
import not.ogame.bots.selenium._
import not.ogame.bots.{BuildingProgress, Resources, SuppliesBuilding, SuppliesPageData, ghostbuster}

class GBot(jitterProvider: RandomTimeJitter, botConfig: BotConfig)(implicit clock: Clock) {
  def nextStep(state: PlanetState.LoggedIn): PlanetState.LoggedIn = {
    println(s"processing next state $state")
    val nextState = state match {
      case loggedState @ PlanetState.LoggedIn(SuppliesPageData(_, _, _, _, _, Some(buildingProgress)), tasks) =>
        scheduleRefreshAfterBuildingFinishes(loggedState, buildingProgress, tasks)
      case loggedState @ PlanetState.LoggedIn(suppliesPage, Nil) =>
        handleWishWhenLogged(loggedState, suppliesPage)
          .map(task => loggedState.modify(_.scheduledTasks).setTo(loggedState.scheduledTasks :+ task))
          .getOrElse(loggedState)
      case other => other
    }
    println(s"calculated next state: $nextState")
    nextState
  }

  private def scheduleRefreshAfterBuildingFinishes(
      loggedState: PlanetState.LoggedIn,
      buildingProgress: BuildingProgress,
      tasks: List[Task]
  ) = {
    tasks.find(_.isInstanceOf[Task.Refresh]) match {
      case None =>
        loggedState
          .modify(_.scheduledTasks)
          .setTo(loggedState.scheduledTasks :+ Task.refresh(buildingProgress.finishTimestamp))
      case _ => loggedState
    }
  }

  private def handleWishWhenLogged(loggedState: PlanetState.LoggedIn, suppliesPage: SuppliesPageData) = {
    botConfig.wishlist
      .collectFirst { case w: Wish.Build if suppliesPage.suppliesLevels.map(w.suppliesBuilding).value < w.level.value => w }
      .flatMap { w =>
        buildBuilding(loggedState, suppliesPage, w)
      }
  }

  private def buildBuilding(
      loggedState: PlanetState.LoggedIn,
      suppliesPage: SuppliesPageData,
      buildWish: Wish.Build
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
        buildStorage(loggedState, suppliesPage, requiredResources)
      }
    }
  }

  private def buildStorage(
      loggedState: PlanetState.LoggedIn,
      suppliesPage: SuppliesPageData,
      requiredResources: Resources
  ): Option[Task.BuildSupply] = {
    requiredResources.difference(suppliesPage.currentCapacity) match {
      case Resources(m, _, _) if m > 0 =>
        buildBuilding(
          loggedState,
          suppliesPage,
          Wish.Build(
            SuppliesBuilding.MetalStorage,
            nextLevel(suppliesPage, SuppliesBuilding.MetalStorage)
          )
        )
      case Resources(_, c, _) if c > 0 =>
        buildBuilding(
          loggedState,
          suppliesPage,
          Wish.Build(
            SuppliesBuilding.CrystalStorage,
            nextLevel(suppliesPage, SuppliesBuilding.CrystalStorage)
          )
        )
      case Resources(_, _, d) if d > 0 =>
        buildBuilding(
          loggedState,
          suppliesPage,
          Wish.Build(
            SuppliesBuilding.DeuteriumStorage,
            nextLevel(suppliesPage, SuppliesBuilding.DeuteriumStorage)
          )
        )
    }
  }

  private def nextLevel(suppliesPage: SuppliesPageData, storage: SuppliesBuilding) = {
    refineVUnsafe[Positive, Int](suppliesPage.suppliesLevels.map(storage).value + 1)
  }
}
