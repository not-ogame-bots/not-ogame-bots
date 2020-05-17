package not.ogame.bots
import cats.effect.Resource
import enumeratum._

trait OgameDriverCreator[F[_]] {
  def create(credentials: Credentials): Resource[F, OgameDriver[F]]
}

case class Credentials(login: String, password: String, universeName: String, universeId: String)

trait OgameDriver[F[_]] {
  def login(): F[Unit]

  def getSuppliesLevels(planetId: String): F[SuppliesLevels]
}

case class SuppliesLevels(map: Map[SuppliesBuilding, Int])

sealed trait SuppliesBuilding extends EnumEntry

object SuppliesBuilding extends Enum[SuppliesBuilding] {

  case object METAL_MINE extends SuppliesBuilding

  case object CRYSTAL_MINE extends SuppliesBuilding

  case object DEUTERIUM_SYNTHESIZER extends SuppliesBuilding

  case object SOLAR_PLANT extends SuppliesBuilding

  case object METAL_STORAGE extends SuppliesBuilding

  case object CRYSTAL_STORAGE extends SuppliesBuilding

  case object DEUTERIUM_STORAGE extends SuppliesBuilding

  val values: IndexedSeq[SuppliesBuilding] = findValues
}

sealed trait FacilityBuilding extends EnumEntry

object FacilityBuilding extends Enum[FacilityBuilding] {

  case object ROBOTICS_FACTORY extends FacilityBuilding

  case object SHIPYARD extends FacilityBuilding

  case object RESEARCH_LAB extends FacilityBuilding

  case object NANITE_FACTORY extends FacilityBuilding

  val values: IndexedSeq[FacilityBuilding] = findValues
}
