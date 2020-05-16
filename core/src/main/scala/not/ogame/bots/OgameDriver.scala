package not.ogame.bots

trait OgameDriver[F[_]] {
  def getFactories(planetId: String): F[PlanetFactories]
}

case class PlanetFactories(metal: Int)
