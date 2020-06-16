package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.{LocalClock, OgameDriver, PlayerPlanet}

import scala.util.Random

class KeepActiveOgameAction[T[_]: Monad](planets: List[PlayerPlanet])(implicit clock: LocalClock) extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    val actions: List[T[Unit]] = Random.shuffle(planets).take(planets.size / 2 + 1).map(it => ogame.readSuppliesPage(it.id).void)
    val joinedActions: T[List[Unit]] = actions.sequence
    joinedActions.map(_ => clock.now().plusMinutes(13).plusSeconds(40))
  }
}
