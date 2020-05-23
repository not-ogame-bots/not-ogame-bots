package not.ogame.bots.ordon

import java.time.Instant

import not.ogame.bots.OgameDriver

case class ScheduledAction[T[_]](resumeOn: Instant, action: OgameAction[T])

trait OgameAction[T[_]] {
  def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]]
}
