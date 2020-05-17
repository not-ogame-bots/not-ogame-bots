package not.ogame.bots

import cats.effect.Resource

trait OgameDriverCreator[F[_]] {
  def create(credentials: Credentials): Resource[F, OgameDriver[F]]
}

case class Credentials(login: String, password: String, universeName: String, universeId: String)

trait OgameDriver[F[_]] {
  def login(): F[Unit]

  def getSuppliesLevels(planetId: String): F[SuppliesLevels]
}

case class SuppliesLevels(map: Map[SuppliesBuilding.Value, Int])

object SuppliesBuilding extends Enumeration {
  type SuppliesBuilding = Value
  val METAL_MINE, CRYSTAL_MINE, DEUTERIUM_SYNTHESIZER, SOLAR_PLANT, METAL_STORAGE, CRYSTAL_STORAGE,
  DEUTERIUM_STORAGE: SuppliesBuilding.Value = Value
}

object FacilityBuilding extends Enumeration {
  type FacilityBuilding = Value
  val ROBOTICS_FACTORY, SHIPYARD, RESEARCH_LAB, NANITE_FACTORY: FacilityBuilding.Value = Value
}
