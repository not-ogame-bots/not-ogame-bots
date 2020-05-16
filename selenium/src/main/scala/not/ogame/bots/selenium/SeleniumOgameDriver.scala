package not.ogame.bots.selenium

import cats.effect.IO
import not.ogame.bots.{OgameDriver, PlanetFactories}

class SeleniumOgameDriver extends OgameDriver[IO] {
  override def getFactories(planetId: String): IO[PlanetFactories] = ???
}
