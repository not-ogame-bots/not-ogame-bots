package not.ogame.bots.ghostbuster

import java.time.{Clock, Instant, ZoneOffset}

import com.softwaremill.quicklens._
import not.ogame.bots.SuppliesPageData
import not.ogame.bots.facts.SuppliesBuildingCosts

class GBot(jitterProvider: RandomTimeJitter)(implicit clock: Clock) {
  def nextStep(state: State): State = {
    println(s"processing next state $state")
    state match {
      case initialState: State.LoggedOut =>
        initialState.modify(_.scheduledTasks).setTo(initialState.scheduledTasks :+ Task.login(clock.instant()))
      case loggedState @ State.LoggedIn(SuppliesPageData(_, _, _, _, _, Some(buildingProgress)), _, tasks) =>
        tasks.find(_.isInstanceOf[Task.Refresh]) match {
          case None =>
            loggedState
              .modify(_.scheduledTasks)
              .setTo(loggedState.scheduledTasks :+ Task.refresh(buildingProgress.finishTimestamp))
          case _ => loggedState
        }
      case loggedState @ State.LoggedIn(suppliesPage, wish :: tail, Nil) =>
        wish match {
          case buildWish: Wish.Build =>
            if (suppliesPage.suppliesLevels.map(buildWish.suppliesBuilding) < buildWish.level.value) {
              buildBuilding(loggedState, suppliesPage, tail, buildWish)
            } else {
              loggedState
                .modify(_.wishList)
                .setTo(tail)
            }
        }
      case other => other
    }
  }

  private def buildBuilding(loggedState: State.LoggedIn, suppliesPage: SuppliesPageData, tail: List[Wish], buildWish: Wish.Build) = {
    val requiredResources = SuppliesBuildingCosts.buildingCost(buildWish.suppliesBuilding, buildWish.level)
    if (suppliesPage.currentResources.gtEqTo(requiredResources)) {
      scheduleWish(loggedState, toTask(buildWish, clock.instant()), tail)
    } else {
      val stillNeed = requiredResources.difference(suppliesPage.currentResources)
      val hoursToWait = stillNeed.div(suppliesPage.currentProduction).max
      val secondsToWait = (hoursToWait * 3600).toInt + jitterProvider.getJitterInSeconds()
      val timeOfExecution = clock.instant().plusSeconds(secondsToWait)
      scheduleWish(loggedState, toTask(buildWish, timeOfExecution), tail)
    }
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
