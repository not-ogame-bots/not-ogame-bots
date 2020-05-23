package not.ogame.bots.ghostbuster.processors

import java.time.Clock
import java.time.temporal.ChronoUnit

import not.ogame.bots.ghostbuster.{PlanetState, Task}
import com.softwaremill.quicklens._

class InactivityProcessor(implicit clock: Clock) {
  def apply(state: PlanetState.LoggedIn): PlanetState.LoggedIn = {
    if (checkAlreadyInQueue[Task.DumpActivity](state.scheduledTasks)) {
      state
    } else {
      state.modify(_.scheduledTasks).setTo(state.scheduledTasks :+ Task.DumpActivity(clock.instant().plus(14, ChronoUnit.MINUTES)))
    }
  }
}
