package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.{LocalClock, OgameDriver, PlayerPlanet}

import scala.util.Random

class KeepActiveOgameAction[T[_]: Monad](planets: List[PlayerPlanet])(implicit clock: LocalClock) extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    val actions: List[T[Unit]] = Random.shuffle(planets).take(planets.size / 2 + 1).map(it => ogame.readSuppliesPage(it.id).map(a => ()))
    val joinedActions: T[Unit] = oneAfterOther(actions)
    joinedActions.map(_ => clock.now().plusMinutes(13).plusSeconds(40))
  }

  private def oneAfterOther(actions: List[T[Unit]]): T[Unit] = {
    val unitT: T[Unit] = ().pure[T]
    actions.fold(unitT) { (a: T[Unit], b: T[Unit]) =>
      a.flatMap(_ => b)
    }
  }
}
