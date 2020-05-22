package not.ogame.bots

import java.time.Instant

import cats.effect.Resource
import enumeratum.EnumEntry.Snakecase
import enumeratum._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative

trait OgameDriverCreator[F[_]] {
  def create(credentials: Credentials): Resource[F, OgameDriver[F]]
}

case class Credentials(login: String, password: String, universeName: String, universeId: String)

trait OgameDriver[F[_]] {
  def login(): F[Unit]

  def readSuppliesPage(planetId: String): F[SuppliesPageData]

  def buildSuppliesBuilding(planetId: String, suppliesBuilding: SuppliesBuilding): F[Unit]

  def readFacilityBuildingsLevels(planetId: String): F[FacilitiesBuildingLevels]

  def buildFacilityBuilding(planetId: String, facilityBuilding: FacilityBuilding): F[Unit]

  def buildShips(planetId: String, shipType: ShipType, count: Int): F[Unit]

  def checkFleetOnPlanet(planetId: String, shipType: ShipType): F[Int]
  
  def readAllFleets(): F[List[Fleet]]
}

case class SuppliesPageData(
    timestamp: Instant,
    currentResources: Resources,
    currentProduction: Resources,
    currentCapacity: Resources,
    suppliesLevels: SuppliesBuildingLevels,
    currentBuildingProgress: Option[BuildingProgress],
    currentShipyardProgress: Option[BuildingProgress]
)

case class Resources(metal: Int, crystal: Int, deuterium: Int, energy: Int = 0) {
  def gtEqTo(requiredResources: Resources): Boolean =
    metal >= requiredResources.metal && crystal >= requiredResources.crystal && deuterium >= requiredResources.deuterium

  def difference(other: Resources): Resources = {
    Resources(Math.max(metal - other.metal, 0), Math.max(crystal - other.crystal, 0), Math.max(deuterium - other.deuterium, 0), 0)
  }

  def div(other: Resources): List[Double] = {
    List(
      divideIfGreaterThanZero(metal, other.metal),
      divideIfGreaterThanZero(crystal, other.crystal),
      divideIfGreaterThanZero(deuterium, other.deuterium)
    )
  }

  private def divideIfGreaterThanZero(first: Double, second: Double) = {
    if (second > 0) {
      first.toDouble / second
    } else if (first > 0) {
      Double.PositiveInfinity
    } else {
      0.0
    }
  }
}

case class SuppliesBuildingLevels(map: Map[SuppliesBuilding, Int Refined NonNegative])

case class FacilitiesBuildingLevels(map: Map[FacilityBuilding, Int Refined NonNegative])

case class BuildingProgress(finishTimestamp: Instant)

sealed trait SuppliesBuilding extends EnumEntry with Snakecase

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

case class PlayerPlanet(id: String, coordinates: Coordinates)

case class Coordinates(galaxy: Int, system: Int, position: Int, coordinatesType: CoordinatesType = CoordinatesType.Planet)

sealed trait CoordinatesType extends EnumEntry

object CoordinatesType extends Enum[CoordinatesType] {
  case object Planet extends CoordinatesType

  case object Moon extends CoordinatesType

  case object Debris extends CoordinatesType

  val values: IndexedSeq[CoordinatesType] = findValues
}


sealed trait ShipType
object ShipType {
  case object LIGHT_FIGHTER extends ShipType
  case object HEAVY_FIGHTER extends ShipType
  case object CRUISER extends ShipType
  case object BATTLESHIP extends ShipType
  case object INTERCEPTOR extends ShipType
  case object DESTROYER extends ShipType
  case object EXPLORER extends ShipType
  case object SMALL_CARGO_SHIP extends ShipType
  case object LARGE_CARGO_SHIP extends ShipType
  case object RECYCLER extends ShipType
  case object ESPIONAGE_PROBE extends ShipType
  case object BOMBER extends ShipType
  case object REAPER extends ShipType

case class Fleet(
    arrivalTime: LocalDateTime,
    fleetAttitude: FleetAttitude,
    fleetMissionType: FleetMissionType,
    from: Coordinates,
    to: Coordinates
)

sealed trait FleetAttitude extends EnumEntry

object FleetAttitude extends Enum[FleetAttitude] {
  case object Friendly extends FleetAttitude

  case object Hostile extends FleetAttitude

  val values: IndexedSeq[FleetAttitude] = findValues
}

sealed trait FleetMissionType extends EnumEntry

object FleetMissionType extends Enum[FleetMissionType] {
  case object Deployment extends FleetMissionType

  case object Expedition extends FleetMissionType

  case object Unknown extends FleetMissionType

  val values: IndexedSeq[FleetMissionType] = findValues
}
