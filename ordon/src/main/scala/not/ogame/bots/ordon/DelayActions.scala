package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.{LocalClock, OgameDriver}

class DelayActions[T[_]: Monad](startOn: ZonedDateTime, actions: List[OgameAction[T]])(implicit clock: LocalClock) extends OgameAction[T] {
  override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] = {
    if (clock.now().isAfter(startOn)) {
      actions.map(action => ScheduledAction(clock.now(), action)).pure[T]
    } else {
      List(ScheduledAction(clock.now().plusMinutes(5), this)).pure[T]
    }
  }
}
