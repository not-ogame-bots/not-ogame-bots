package not.ogame.bots.ghostbuster

import java.time.Clock

import not.ogame.bots.ghostbuster.processors.{BuildMtUpToCapacityProcessor, InactivityProcessor, WishlistProcessor}

class GBot(jitterProvider: RandomTimeJitter, botConfig: BotConfig)(implicit clock: Clock) {
  println(s"Creating with botConfig: $botConfig")

  private val wishlistProcessor = new WishlistProcessor(botConfig, jitterProvider)
  private val buildMtUpToCapacityProcessor = new BuildMtUpToCapacityProcessor(botConfig, jitterProvider)
  private val inactivityProcessor = new InactivityProcessor(botConfig)

  def nextStep(state: State.LoggedIn): State.LoggedIn = {
    println("processing state:")
    pprint.pprintln(state)
    val nextState =
      List(wishlistProcessor(_), buildMtUpToCapacityProcessor(_), inactivityProcessor(_)).foldLeft(state)((acc, item) => item(acc))
    println(s"calculated next state: ${nextState.planets.head.suppliesPage.timestamp}")
    pprint.pprintln(nextState.scheduledTasks)
    nextState
  }
}
