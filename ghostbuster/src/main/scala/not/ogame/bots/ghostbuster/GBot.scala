package not.ogame.bots.ghostbuster

import java.time.{Clock, Instant, LocalDateTime}

import com.softwaremill.quicklens._
import not.ogame.bots.facts.SuppliesBuildingCosts

class GBot(jitterProvider: RandomTimeJitter)(implicit clock: Clock) {
  def nextStep(state: State): State = {
    state.wishList match {
      case (wish: Wish.Build) :: tail if state.scheduledWishes.isEmpty =>
        val requiredResources = SuppliesBuildingCosts.buildingCost(wish.suppliesBuilding, wish.level)
        if (state.suppliesPage.currentResources.gtEqTo(requiredResources)) {
          scheduleWish(state, wish, tail, clock.instant())
        } else {
          val difference = state.suppliesPage.currentResources.difference(requiredResources)
          val hoursToWait = difference.div(state.suppliesPage.currentProduction).max
          val secondsToWait = (hoursToWait * 3600).toInt + jitterProvider.getJitterInSeconds()
          val timeOfExecution = clock.instant().plusSeconds(secondsToWait)
          scheduleWish(state, wish, tail, timeOfExecution)
        }
      case _ => state
    }
  }

  private def scheduleWish(state: State, wish: Wish.Build, tail: List[Wish], timeOfExecution: Instant) = {
    state
      .modify(_.scheduledWishes)
      .setTo(state.scheduledWishes :+ (timeOfExecution -> wish))
      .modify(_.wishList)
      .setTo(tail)
  }
}
