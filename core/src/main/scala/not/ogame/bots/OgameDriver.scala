package not.ogame.bots

import java.time.LocalDateTime

import cats.effect.Resource
import enumeratum._

trait OgameDriverCreator[F[_]] {
  def create(credentials: Credentials): Resource[F, OgameDriver[F]]
}

case class Credentials(login: String, password: String, universeName: String, universeId: String)

trait OgameDriver[F[_]] {
  def login(): F[Unit]

  def readSuppliesPage(planetId: String): F[SuppliesPageData]

  def buildSuppliesBuilding(planetId: String, suppliesBuilding: SuppliesBuilding): F[Unit]
}

case class SuppliesPageData(
    timestamp: LocalDateTime,
    currentResources: Resources,
    currentProduction: Resources,
    suppliesLevels: SuppliesBuildingLevels,
    currentBuildingProgress: Option[BuildingProgress]
)

case class Resources(metal: Int, crystal: Int, deuterium: Int) {
  def gtEqTo(requiredResources: Resources): Boolean =
    metal >= requiredResources.metal && crystal >= requiredResources.crystal && deuterium >= requiredResources.deuterium

  def difference(other: Resources): Resources = {
    Resources(Math.abs(metal - other.metal), Math.abs(metal - other.metal), Math.abs(deuterium - other.deuterium))
  }

  def div(other: Resources): List[Double] = {
    List(metal.toDouble / other.metal, crystal.toDouble / other.crystal, deuterium.toDouble / other.deuterium)
  }
}

case class SuppliesBuildingLevels(map: Map[SuppliesBuilding, Int])

case class BuildingProgress(finishTimestamp: LocalDateTime)

sealed trait SuppliesBuilding extends EnumEntry

object SuppliesBuilding extends Enum[SuppliesBuilding] {
  case object MetalMine extends SuppliesBuilding

  case object CrystalMine extends SuppliesBuilding

  case object DeuteriumSynthesizer extends SuppliesBuilding

  case object SolarPlant extends SuppliesBuilding

  case object MetalStorage extends SuppliesBuilding

  case object CrystalStorage extends SuppliesBuilding

  case object DeuteriumStorage extends SuppliesBuilding

  val values: IndexedSeq[SuppliesBuilding] = findValues
}

sealed trait FacilityBuilding extends EnumEntry
object FacilityBuilding extends Enum[FacilityBuilding] {
  case object RoboticsFactory extends FacilityBuilding

  case object Shipyard extends FacilityBuilding

  case object ResearchLab extends FacilityBuilding

  case object NaniteFactory extends FacilityBuilding

  val values: IndexedSeq[FacilityBuilding] = findValues
}
