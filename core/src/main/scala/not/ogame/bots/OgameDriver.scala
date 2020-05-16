package not.ogame.bots

trait OgameDriverCreator[F[_]] {
  def create(credentials: Credentials) : OgameDriver[F]
}

case class Credentials(login: String, password: String, universeName: String, universeId: String)

trait OgameDriver[F[_]] {
  def login(): F[Unit]
  def getFactories(planetId: String): F[PlanetFactories]
}

case class PlanetFactories(metal: Int)
