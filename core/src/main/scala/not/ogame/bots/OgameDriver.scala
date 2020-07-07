package not.ogame.bots

import java.time.ZonedDateTime

import enumeratum.EnumEntry.Snakecase
import enumeratum._
import not.ogame.bots.FleetSpeed.Percent100

case class Credentials(login: String, password: String, universeName: String, universeId: String)

trait OgameDriver[F[_]] {
  def login(): F[Unit]

  def readSuppliesPage(planetId: PlanetId): F[SuppliesPageData]

  def buildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding): F[Unit]

  def readFacilityPage(planetId: PlanetId): F[FacilityPageData]

  def buildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding): F[Unit]

  def readTechnologyPage(planetId: PlanetId): F[TechnologyPageData]

  def startResearch(planetId: PlanetId, technology: Technology): F[Unit]

  def buildShips(planetId: PlanetId, shipType: ShipType, count: Int): F[Unit]

  def buildSolarSatellites(planetId: PlanetId, count: Int): F[Unit]

  def readFleetPage(planetId: PlanetId): F[FleetPageData]

  def readAllFleets(): F[List[Fleet]]

  def readMyFleets(): F[MyFleetPageData]

  def sendFleet(sendFleetRequest: SendFleetRequest): F[Unit]

  def returnFleet(fleetId: FleetId): F[Unit]

  def readPlanets(): F[List[PlayerPlanet]]

  def checkIsLoggedIn(): F[Boolean]

  def readMyOffers(): F[List[MyOffer]]

  def createOffer(planetId: PlanetId, newOffer: MyOffer): F[Unit]

  def readGalaxyPage(planetId: PlanetId, galaxy: Int, system: Int): F[GalaxyPageData]
}

case class SuppliesPageData(
    timestamp: ZonedDateTime,
    currentResources: Resources,
    currentProduction: Resources,
    currentCapacity: Resources,
    suppliesIntLevels: SuppliesBuildingIntLevels,
    currentBuildingProgress: Option[BuildingProgress],
    currentShipyardProgress: Option[BuildingProgress]
) {
  def buildingInProgress: Boolean = currentBuildingProgress.isDefined

  def shipInProgress: Boolean = currentShipyardProgress.isDefined

  def isBusy: Boolean = buildingInProgress || shipInProgress

  def isIdle: Boolean = !isBusy

  def getIntLevel(suppliesBuilding: SuppliesBuilding): Int = {
    suppliesIntLevels.values(suppliesBuilding)
  }
}

case class FacilityPageData(
    timestamp: ZonedDateTime,
    currentResources: Resources,
    currentProduction: Resources,
    currentCapacity: Resources,
    facilityIntLevels: FacilitiesBuildingIntLevels,
    currentBuildingProgress: Option[BuildingProgress]
) {
  def getIntLevel(facilityBuilding: FacilityBuilding): Int = {
    facilityIntLevels.values(facilityBuilding)
  }
}

case class FleetPageData(
    timestamp: ZonedDateTime,
    currentResources: Resources,
    currentProduction: Resources,
    currentCapacity: Resources,
    slots: FleetSlots,
    ships: Map[ShipType, Int]
)

case class TechnologyPageData(
    timestamp: ZonedDateTime,
    currentResources: Resources,
    currentProduction: Resources,
    currentCapacity: Resources,
    technologyIntLevels: TechnologyIntLevels,
    currentResearchProgress: Option[BuildingProgress]
) {
  def getIntLevel(technology: Technology): Int = {
    technologyIntLevels.values(technology)
  }
}

case class Resources(metal: Int, crystal: Int, deuterium: Int, energy: Int = 0) {
  def multiply(amount: Double): Resources = {
    Resources((metal * amount).toInt, (crystal * amount).toInt, (deuterium * amount).toInt, energy)
  }

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

  def add(other: Resources): Resources = {
    Resources(metal + other.metal, crystal + other.crystal, deuterium + other.deuterium)
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

case class SuppliesBuildingIntLevels(values: Map[SuppliesBuilding, Int])

case class FacilitiesBuildingIntLevels(values: Map[FacilityBuilding, Int])

case class TechnologyIntLevels(values: Map[Technology, Int])

case class BuildingProgress(finishTimestamp: ZonedDateTime, name: String)

sealed trait SuppliesBuilding extends EnumEntry with Snakecase

object SuppliesBuilding extends Enum[SuppliesBuilding] {
  case object MetalMine extends SuppliesBuilding

  case object CrystalMine extends SuppliesBuilding

  case object DeuteriumSynthesizer extends SuppliesBuilding

  case object SolarPlant extends SuppliesBuilding

  case object MetalStorage extends SuppliesBuilding

  case object CrystalStorage extends SuppliesBuilding

  case object DeuteriumStorage extends SuppliesBuilding

  case object FusionPlant extends SuppliesBuilding

  val values: IndexedSeq[SuppliesBuilding] = findValues
}

sealed trait FacilityBuilding extends EnumEntry with Snakecase
object FacilityBuilding extends Enum[FacilityBuilding] {
  case object RoboticsFactory extends FacilityBuilding

  case object Shipyard extends FacilityBuilding

  case object ResearchLab extends FacilityBuilding

  case object NaniteFactory extends FacilityBuilding

  val values: IndexedSeq[FacilityBuilding] = findValues
}

case class PlayerPlanet(id: PlanetId, coordinates: Coordinates)

case class Coordinates(galaxy: Int, system: Int, position: Int, coordinatesType: CoordinatesType = CoordinatesType.Planet)

sealed trait CoordinatesType extends EnumEntry

object CoordinatesType extends Enum[CoordinatesType] {
  case object Planet extends CoordinatesType

  case object Moon extends CoordinatesType

  case object Debris extends CoordinatesType

  val values: IndexedSeq[CoordinatesType] = findValues
}

sealed trait ShipType extends EnumEntry with Snakecase

object ShipType extends Enum[ShipType] {
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

case class MyFleetPageData(fleets: List[MyFleet], fleetSlots: MyFleetSlots)

case class MyFleetSlots(currentFleets: Int, maxFleets: Int, currentExpeditions: Int, maxExpeditions: Int) {
  val isAtLeastOneSlotAvailable: Boolean = currentFleets < maxFleets
}

case class MyFleet(
    fleetId: FleetId,
    arrivalTime: ZonedDateTime,
    fleetMissionType: FleetMissionType,
    from: Coordinates,
    to: Coordinates,
    isReturning: Boolean,
    ships: Map[ShipType, Int],
    isReturnable: Boolean
)

case class Fleet(
    arrivalTime: ZonedDateTime,
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

  case object Colonization extends FleetMissionType

  case object Transport extends FleetMissionType

  case object Attack extends FleetMissionType

  case object Spy extends FleetMissionType

  case object Destroy extends FleetMissionType

  case object Recycle extends FleetMissionType

  case object Unknown extends FleetMissionType

  val values: IndexedSeq[FleetMissionType] = findValues
}

case class SendFleetRequest(
    from: PlayerPlanet,
    ships: SendFleetRequestShips,
    targetCoordinates: Coordinates,
    fleetMissionType: FleetMissionType,
    resources: FleetResources,
    speed: FleetSpeed = Percent100
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

sealed trait FleetSpeed extends EnumEntry with Snakecase

object FleetSpeed extends Enum[FleetSpeed] {
  case object Percent10 extends FleetSpeed

  case object Percent20 extends FleetSpeed

  case object Percent30 extends FleetSpeed

  case object Percent40 extends FleetSpeed

  case object Percent50 extends FleetSpeed

  case object Percent60 extends FleetSpeed

  case object Percent70 extends FleetSpeed

  case object Percent80 extends FleetSpeed

  case object Percent90 extends FleetSpeed

  case object Percent100 extends FleetSpeed

  val values: IndexedSeq[FleetSpeed] = findValues
}

sealed trait Technology extends EnumEntry with Snakecase

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

case class MyOffer(
    offerItemType: OfferItemType,
    amount: Int,
    priceItemType: OfferItemType,
    price: Int
)

sealed trait OfferItemType extends EnumEntry

object OfferItemType extends Enum[OfferItemType] {
  case object Metal extends OfferItemType

  case object Crystal extends OfferItemType

  case object Deuterium extends OfferItemType

  val values: IndexedSeq[OfferItemType] = findValues
}

case class FleetSlots(
    currentFleets: Int,
    maxFleets: Int,
    currentExpeditions: Int,
    maxExpeditions: Int,
    currentTradeFleets: Int,
    maxTradeFleets: Int
)

case class GalaxyPageData(
    playerActivityMap: Map[Coordinates, PlayerActivity],
    debrisMap: Map[Coordinates, Resources]
)

sealed trait PlayerActivity extends EnumEntry

object PlayerActivity extends Enum[PlayerActivity] {
  case object LessThan15MinutesAgo extends PlayerActivity

  case class MinutesAgo(minutes: Int) extends PlayerActivity

  case object NotActive extends PlayerActivity

  override def values: IndexedSeq[PlayerActivity] = findValues
}
