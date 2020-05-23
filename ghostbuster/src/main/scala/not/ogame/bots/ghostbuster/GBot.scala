package not.ogame.bots.ghostbuster

import java.time.Clock

import not.ogame.bots.ghostbuster.processors.{BuildMtUpToCapacityProcessor, WishlistProcessor}

class GBot(jitterProvider: RandomTimeJitter, botConfig: BotConfig)(implicit clock: Clock) {
  println(s"Creating with botConfig: $botConfig")

  private val wishlistProcessor = new WishlistProcessor(botConfig, jitterProvider)
  private val buildMtUpToCapacityProcessor = new BuildMtUpToCapacityProcessor(botConfig, jitterProvider)

  def nextStep(state: PlanetState.LoggedIn): PlanetState.LoggedIn = {
    println(s"processing next state $state")
    val nextState = List(wishlistProcessor.apply(_), buildMtUpToCapacityProcessor.apply(_)).foldLeft(state)((acc, item) => item(acc))
    println(s"calculated next state: ${nextState.suppliesPage.timestamp} ${nextState.scheduledTasks}")
    nextState
  }
}
