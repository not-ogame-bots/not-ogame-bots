package not.ogame.bots.ordon

import java.time.LocalDateTime

import cats.Monad
import not.ogame.bots.OgameDriver

case class ScheduledAction[T[_]: Monad](resumeOn: LocalDateTime, action: OgameAction[T]) {
  def process(ogame: OgameDriver[T], now: LocalDateTime): T[List[ScheduledAction[T]]] = {
    if (resumeOn.isBefore(now)) {
      action.process(ogame)
    } else {
      Monad[T].pure(List(this))
    }
  }
}

trait OgameAction[T[_]] {
  def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]]
}
