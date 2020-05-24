package not.ogame.bots.ordon

import java.time.Instant

import cats.Monad
import cats.implicits._
import not.ogame.bots.OgameDriver

case class ScheduledAction[T[_]](resumeOn: Instant, action: OgameAction[T])

trait OgameAction[T[_]] {
  def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]]
}

abstract class SimpleOgameAction[T[_]: Monad] extends OgameAction[T] {
  def processSimple(ogame: OgameDriver[T]): T[Instant]

  final def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] =
    processSimple(ogame).map(resumeOn => List(ScheduledAction(resumeOn, this)))
}
