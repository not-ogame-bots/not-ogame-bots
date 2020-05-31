package not.ogame.bots

import java.time.ZonedDateTime

import cats.effect.Resource
import enumeratum.EnumEntry.Snakecase
import enumeratum._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import not.ogame.bots.FleetSpeed.Percent100

trait OgameDriverCreator[F[_]] {
  def create(credentials: Credentials): Resource[F, OgameDriver[F]]
}

case class Credentials(login: String, password: String, universeName: String, universeId: String)

trait OgameDriver[F[_]] {
  def login(): F[Unit]

  def readSuppliesPage(planetId: String): F[SuppliesPageData]

  def buildSuppliesBuilding(planetId: String, suppliesBuilding: SuppliesBuilding): F[Unit]

  def readFacilityPage(planetId: String): F[FacilityPageData]

  def buildFacilityBuilding(planetId: String, facilityBuilding: FacilityBuilding): F[Unit]

  def buildShips(planetId: String, shipType: ShipType, count: Int): F[Unit]

  def checkFleetOnPlanet(planetId: String): F[Map[ShipType, Int]]

  def readAllFleets(): F[List[Fleet]]

  def readMyFleets(): F[List[MyFleet]]

  def sendFleet(sendFleetRequest: SendFleetRequest): F[Unit]

  def returnFleet(fleetId: FleetId): F[Unit]

  def readPlanets(): F[List[PlayerPlanet]]

  def checkIsLoggedIn(): F[Boolean]
}

case class SuppliesPageData(
    timestamp: ZonedDateTime,
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

  def getLevel(suppliesBuilding: SuppliesBuilding): Int Refined NonNegative = {
    suppliesLevels.values(suppliesBuilding)
  }
}

case class FacilityPageData(
    timestamp: ZonedDateTime,
    currentResources: Resources,
    currentProduction: Resources,
    currentCapacity: Resources,
    facilityLevels: FacilitiesBuildingLevels,
    currentBuildingProgress: Option[BuildingProgress]
) {
  def getLevel(facilityBuilding: FacilityBuilding): Int Refined NonNegative = {
    facilityLevels.values(facilityBuilding)
  }
}

case class Resources(metal: Int, crystal: Int, deuterium: Int, energy: Int = 0) {
  def multiply(amount: Int): Resources = {
    Resources(metal * amount, crystal * amount, deuterium * amount, energy)
  }

  def gtEqTo(requiredResources: Resources): Boolean =
    metal >= requiredResources.metal && crystal >= requiredResources.crystal && deuterium >= requiredResources.deuterium

  def difference(other: Resources): Resources = {
    Resources(Math.max(metal - other.metal, 0), Math.max(crystal - other.crystal, 0), Math.max(deuterium - other.deuterium, 0))
  }

  def div(other: Resources): (Double, Double, Double) = {
    (
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

case class SuppliesBuildingLevels(values: Map[SuppliesBuilding, Int Refined NonNegative])

case class FacilitiesBuildingLevels(values: Map[FacilityBuilding, Int Refined NonNegative])

case class BuildingProgress(finishTimestamp: ZonedDateTime)

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

case class MyFleet(
    fleetId: FleetId,
    arrivalTime: ZonedDateTime,
    fleetMissionType: FleetMissionType,
    from: Coordinates,
    to: Coordinates,
    isReturning: Boolean
)

case class Fleet(
    arrivalTime: SimplifiedDataTime,
    fleetAttitude: FleetAttitude,
    fleetMissionType: FleetMissionType,
    from: Coordinates,
    to: Coordinates,
    isReturning: Boolean
)

case class SimplifiedDataTime(hour: Int, minutes: Int, seconds: Int, day: Int, month: Int, year: Int) {
  def toZdt(implicit clock: LocalClock): ZonedDateTime = {
    clock
      .now()
      .withHour(hour)
      .withMinute(minutes)
      .withSecond(seconds)
      .withDayOfMonth(day)
      .withMonth(month)
      .withYear(year)
  }

  override def toString: String = {
    s"$year:$month:$day $hour$minutes$seconds"
  }
}
object SimplifiedDataTime {
  def from(zdt: ZonedDateTime): SimplifiedDataTime = {
    SimplifiedDataTime(zdt.getHour, zdt.getMinute, zdt.getSecond, zdt.getDayOfMonth, zdt.getMonthValue, zdt.getYear)
  }

  implicit def toZdt(sdt: SimplifiedDataTime)(implicit clock: LocalClock): ZonedDateTime = {
    import sdt._
    clock
      .now()
      .withHour(hour)
      .withMinute(minutes)
      .withSecond(seconds)
      .withDayOfMonth(day)
      .withMonth(month)
      .withYear(year)
  }

  implicit def ordering(implicit zdtOrdering: Ordering[ZonedDateTime], clock: LocalClock): Ordering[SimplifiedDataTime] = {
    (x: SimplifiedDataTime, y: SimplifiedDataTime) =>
      {
        zdtOrdering.compare(x.toZdt, y.toZdt)
      }
  }
}

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
