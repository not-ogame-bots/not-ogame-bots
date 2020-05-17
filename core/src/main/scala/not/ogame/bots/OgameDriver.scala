package not.ogame.bots

import cats.effect.Resource

trait OgameDriverCreator[F[_]] {
  def create(credentials: Credentials): Resource[F, OgameDriver[F]]
}

case class Credentials(login: String, password: String, universeName: String, universeId: String)

trait OgameDriver[F[_]] {
  def login(): F[Unit]
  def getFactories(planetId: String): F[PlanetFactories]
}

case class PlanetFactories(metal: Int)
