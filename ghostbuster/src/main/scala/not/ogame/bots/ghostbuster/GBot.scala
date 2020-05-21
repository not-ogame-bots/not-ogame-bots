package not.ogame.bots.ghostbuster

import java.time.{Clock, Instant, ZoneOffset}

import com.softwaremill.quicklens._
import eu.timepit.refined.numeric.Positive
import not.ogame.bots.SuppliesBuilding.MetalStorage
import not.ogame.bots.{BuildingProgress, Resources, SuppliesBuilding, SuppliesPageData}
import not.ogame.bots.facts.SuppliesBuildingCosts
import not.ogame.bots.selenium._

class GBot(jitterProvider: RandomTimeJitter)(implicit clock: Clock) {
  def nextStep(state: State): State = {
    println(s"processing next state $state")
    state match {
      case initialState: State.LoggedOut =>
        initialState.modify(_.scheduledTasks).setTo(initialState.scheduledTasks :+ Task.login(clock.instant()))
      case loggedState @ State.LoggedIn(SuppliesPageData(_, _, _, _, _, Some(buildingProgress)), _, tasks) =>
        scheduleRefreshAfterBuildingFinishes(loggedState, buildingProgress, tasks)
      case loggedState @ State.LoggedIn(suppliesPage, wish :: tail, Nil) =>
        handleWishWhenLogged(loggedState, suppliesPage, wish, tail)
      case other => other
    }
  }

  private def handleWishWhenLogged(loggedState: State.LoggedIn, suppliesPage: SuppliesPageData, wish: Wish, tail: List[Wish]) = {
    wish match {
      case buildWish: Wish.Build =>
        if (suppliesPage.suppliesLevels.map(buildWish.suppliesBuilding).value < buildWish.level.value) {
          buildBuilding(loggedState, suppliesPage, tail, buildWish)
        } else {
          loggedState
            .modify(_.wishList)
            .setTo(tail)
        }
    }
  }

  private def scheduleRefreshAfterBuildingFinishes(loggedState: State.LoggedIn, buildingProgress: BuildingProgress, tasks: List[Task]) = {
    tasks.find(_.isInstanceOf[Task.Refresh]) match {
      case None =>
        loggedState
          .modify(_.scheduledTasks)
          .setTo(loggedState.scheduledTasks :+ Task.refresh(buildingProgress.finishTimestamp))
      case _ => loggedState
    }
  }

  private def buildBuilding(
      loggedState: State.LoggedIn,
      suppliesPage: SuppliesPageData,
      tail: List[Wish],
      buildWish: Wish.Build
  ): State.LoggedIn = {
    val requiredResources = SuppliesBuildingCosts.buildingCost(buildWish.suppliesBuilding, buildWish.level)
    if (suppliesPage.currentResources.gtEqTo(requiredResources)) {
      scheduleWish(loggedState, toTask(buildWish, clock.instant()), tail)
    } else {
      if (suppliesPage.currentCapacity.gtEqTo(requiredResources)) {
        val stillNeed = requiredResources.difference(suppliesPage.currentResources)
        val hoursToWait = stillNeed.div(suppliesPage.currentProduction).max
        val secondsToWait = (hoursToWait * 3600).toInt + jitterProvider.getJitterInSeconds()
        val timeOfExecution = clock.instant().plusSeconds(secondsToWait)
        scheduleWish(loggedState, toTask(buildWish, timeOfExecution), tail)
      } else {
        buildStorage(loggedState, suppliesPage, tail, buildWish, requiredResources)
      }
    }
  }

  private def buildStorage(
      loggedState: State.LoggedIn,
      suppliesPage: SuppliesPageData,
      tail: List[Wish],
      buildWish: Wish.Build,
      requiredResources: Resources
  ) = {
    requiredResources.difference(suppliesPage.currentCapacity) match {
      case Resources(m, _, _) if m > 0 =>
        buildBuilding(
          loggedState,
          suppliesPage,
          buildWish :: tail,
          Wish.Build(
            SuppliesBuilding.MetalStorage,
            nextLevel(suppliesPage, SuppliesBuilding.MetalStorage)
          )
        )
      case Resources(_, c, _) if c > 0 =>
        buildBuilding(
          loggedState,
          suppliesPage,
          buildWish :: tail,
          Wish.Build(
            SuppliesBuilding.CrystalStorage,
            nextLevel(suppliesPage, SuppliesBuilding.CrystalStorage)
          )
        )
      case Resources(_, _, d) if d > 0 =>
        buildBuilding(
          loggedState,
          suppliesPage,
          buildWish :: tail,
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

  private def toTask(buildWish: Wish.Build, timeOfExecution: Instant) = {
    Task.build(buildWish.suppliesBuilding, buildWish.level, timeOfExecution)
  }

  private def scheduleWish(state: State.LoggedIn, task: Task, tail: List[Wish]) = {
    state
      .modify(_.scheduledTasks)
      .setTo(state.scheduledTasks :+ task)
      .modify(_.wishList)
      .setTo(tail)
  }
}
