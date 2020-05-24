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

  def checkFleetOnPlanet(planetId: String): F[Map[ShipType, Int]]

  def readAllFleets(): F[List[Fleet]]

  def sendFleet(sendFleetRequest: SendFleetRequest): F[Unit]

  def readPlanets(): F[List[PlayerPlanet]]
}

case class SuppliesPageData(
    timestamp: Instant,
    currentResources: Resources,
    currentProduction: Resources,
    currentCapacity: Resources,
    suppliesLevels: SuppliesBuildingLevels,
    currentBuildingProgress: Option[BuildingProgress],
    currentShipyardProgress: Option[BuildingProgress]
) {
  def buildingInProgress: Boolean = currentBuildingProgress.isDefined
  def shipInProgress: Boolean = currentShipyardProgress.isDefined
  def isBusy: Boolean = buildingInProgress || shipInProgress
  def isIdle: Boolean = !isBusy

  def getLevel(suppliesBuilding: SuppliesBuilding): Int = {
    suppliesLevels.values(suppliesBuilding).value
  }
}

case class Resources(metal: Int, crystal: Int, deuterium: Int, energy: Int = 0) {
  def gtEqTo(requiredResources: Resources): Boolean =
    metal >= requiredResources.metal && crystal >= requiredResources.crystal && deuterium >= requiredResources.deuterium

  def difference(other: Resources): Resources = {
    Resources(Math.max(metal - other.metal, 0), Math.max(crystal - other.crystal, 0), Math.max(deuterium - other.deuterium, 0))
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

object Resources {
  def Zero: Resources = Resources(0, 0, 0)
}

case class SuppliesBuildingLevels(values: Map[SuppliesBuilding, Int Refined NonNegative])

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

sealed trait ShipType extends EnumEntry

object ShipType extends Enum[ShipType] with Snakecase {
  case object LightFighter extends ShipType

  case object HeavyFighter extends ShipType

  case object Cruiser extends ShipType

  case object Battleship extends ShipType

  case object Interceptor extends ShipType

  case object Bomber extends ShipType

  case object Destroyer extends ShipType

  case object DeathStar extends ShipType

  case object Reaper extends ShipType

  case object Explorer extends ShipType

  case object SmallCargoShip extends ShipType

  case object LargeCargoShip extends ShipType

  case object ColonyShip extends ShipType

  case object Recycler extends ShipType

  case object EspionageProbe extends ShipType

  override def values: IndexedSeq[ShipType] = findValues
}

case class Fleet(
    arrivalTime: Instant,
    fleetAttitude: FleetAttitude,
    fleetMissionType: FleetMissionType,
    from: Coordinates,
    to: Coordinates,
    isReturning: Boolean
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

case class SendFleetRequest(
    startPlanetId: String,
    ships: SendFleetRequestShips,
    targetCoordinates: Coordinates,
    fleetMissionType: FleetMissionType,
    resources: FleetResources
)

sealed trait FleetResources

object FleetResources {
  case class Given(resources: Resources) extends FleetResources
  case object Max extends FleetResources
}

sealed trait SendFleetRequestShips

object SendFleetRequestShips {
  case object AllShips extends SendFleetRequestShips

  case class Ships(ships: Map[ShipType, Int]) extends SendFleetRequestShips
}

sealed trait Technology extends EnumEntry

object Technology extends Enum[Technology] {
  case object Energy extends Technology

  case object Laser extends Technology

  case object Ion extends Technology

  case object Hyperspace extends Technology

  case object Plasma extends Technology

  case object CombustionDrive extends Technology

  case object ImpulseDrive extends Technology

  case object HyperspaceDrive extends Technology

  case object Espionage extends Technology

  case object Computer extends Technology

  case object Astrophysics extends Technology

  case object ResearchNetwork extends Technology

  case object Graviton extends Technology

  case object Weapons extends Technology

  case object Shielding extends Technology

  case object Armor extends Technology

  val values: IndexedSeq[Technology] = findValues
}
