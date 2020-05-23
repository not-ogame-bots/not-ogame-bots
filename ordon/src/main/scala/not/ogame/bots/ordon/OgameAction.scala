package not.ogame.bots.ordon

import java.time.LocalDateTime

import not.ogame.bots.OgameDriver

case class ScheduledAction[T[_]](resumeOn: LocalDateTime, action: OgameAction[T])

trait OgameAction[T[_]] {
  def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]]
}
