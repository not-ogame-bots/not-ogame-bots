package not.ogame.bots.ghostbuster.processors

import java.time.Clock
import java.time.temporal.ChronoUnit

import com.softwaremill.quicklens._
//import not.ogame.bots.ghostbuster.{BotConfig, State, Action}
//
//class InactivityProcessor(botConfig: BotConfig)(implicit clock: Clock) {
//  def apply(state: State.LoggedIn): State.LoggedIn = {
//    if (!checkAlreadyInQueue(state.scheduledTasks) { case t: Action.DumpActivity => t } && botConfig.activityFaker) {
//      state
//        .modify(_.scheduledTasks)
//        .setTo(state.scheduledTasks :+ Action.DumpActivity(clock.instant().plus(14, ChronoUnit.MINUTES), state.planets.map(_.id)))
//    } else {
//      state
//    }
//  }
//}
