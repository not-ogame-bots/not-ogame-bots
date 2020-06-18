package not.ogame.bots.ordon

import cats.Monad
import cats.implicits._
import not.ogame.bots.{LocalClock, OgameDriver}

class StartBuildingsOgameAction[T[_]: Monad](implicit clock: LocalClock) extends OgameAction[T] {
  override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] =
    for {
      planets <- ogame.readPlanets()
    } yield List(ScheduledAction(clock.now(), new BuildBuildingsOgameAction[T](planets.head)))
}
