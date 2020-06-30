package not.ogame.bots.ordon.action

import java.time.ZonedDateTime

import cats.implicits._
import not.ogame.bots.PlayerPlanet
import not.ogame.bots.ordon.core.{EventRegistry, OrdonOgameDriver, TimeBasedOrdonAction}

import scala.util.Random

class KeepActiveOrdonAction(planets: List[PlayerPlanet]) extends TimeBasedOrdonAction {
  override def processTimeBased(ogame: OrdonOgameDriver, eventRegistry: EventRegistry): ZonedDateTime = {
    Random.shuffle(planets).take(planets.size / 2 + 1).foreach(planet => ogame.readSuppliesPage(planet.id))
    ZonedDateTime.now().plusMinutes(13).plusSeconds(40)
  }
}
