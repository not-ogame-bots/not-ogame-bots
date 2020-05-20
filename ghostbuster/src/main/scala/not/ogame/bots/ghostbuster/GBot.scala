package not.ogame.bots.ghostbuster

import java.time.{Clock, Instant}

import com.softwaremill.quicklens._
import not.ogame.bots.facts.SuppliesBuildingCosts

class GBot(jitterProvider: RandomTimeJitter)(implicit clock: Clock) {
  def nextStep(state: State): State = {
    state match {
      case initialState: State.LoggedOut =>
        initialState.modify(_.scheduledTasks).setTo(initialState.scheduledTasks :+ Task.login(clock.instant()))
      case loggedState @ State.LoggedIn(suppliesPage, wish :: tail, Nil) =>
        wish match {
          case buildWish: Wish.Build =>
            val requiredResources = SuppliesBuildingCosts.buildingCost(buildWish.suppliesBuilding, buildWish.level)
            if (suppliesPage.currentResources.gtEqTo(requiredResources)) {
              scheduleWish(loggedState, toTask(buildWish, clock.instant()), tail)
            } else {
              val difference = suppliesPage.currentResources.difference(requiredResources)
              val hoursToWait = difference.div(suppliesPage.currentProduction).max
              val secondsToWait = (hoursToWait * 3600).toInt + jitterProvider.getJitterInSeconds()
              val timeOfExecution = clock.instant().plusSeconds(secondsToWait)
              scheduleWish(loggedState, toTask(buildWish, timeOfExecution), tail)
            }
        }
      case other => other
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
