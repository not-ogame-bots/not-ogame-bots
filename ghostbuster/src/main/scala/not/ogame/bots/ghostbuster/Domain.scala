package not.ogame.bots.ghostbuster

import not.ogame.bots._

import scala.concurrent.duration.FiniteDuration

sealed trait Wish {
  def planetId: PlanetId
}
object Wish {
  case class BuildSupply(suppliesBuilding: SuppliesBuilding, level: Int, planetId: PlanetId) extends Wish

  case class BuildFacility(facilityBuilding: FacilityBuilding, level: Int, planetId: PlanetId) extends Wish

  case class BuildShip(shipType: ShipType, planetId: PlanetId, amount: Int) extends Wish

  case class SmartSupplyBuilder(
      metalLevel: Int,
      crystalLevel: Int,
      deuterLevel: Int,
      planetId: PlanetId
  ) extends Wish

  case class Research(technology: Technology, level: Int, planetId: PlanetId) extends Wish

  case class DeuterBuilder(level: Int, planetId: PlanetId) extends Wish
}

case class BotConfig(
    wishlist: List[Wish],
    fsConfig: FsConfig,
    expeditionConfig: ExpeditionConfig,
    smartBuilder: SmartBuilderConfig,
    escapeConfig: EscapeConfig,
    flyAndReturn: FlyAndReturnConfig
)

case class SmartBuilderConfig(
    interval: FiniteDuration,
    isOn: Boolean
)

case class FsConfig(
    ships: List[FleetShip],
    isOn: Boolean,
    searchInterval: FiniteDuration,
    remainDeuterAmount: Int,
    takeResources: Boolean,
    gatherShips: Boolean,
    fleetSpeed: FleetSpeed,
    eligiblePlanets: List[PlanetId],
    builder: Boolean,
    maxWaitTime: FiniteDuration,
    maxBuildingTime: FiniteDuration
)
case class ExpeditionConfig(
    isOn: Boolean,
    maxNumberOfExpeditions: Int,
    startingPlanetId: PlanetId,
    collectingPlanet: PlanetId,
    collectingOn: Boolean,
    target: Coordinates,
    maxLC: Int,
    maxSC: Int
)
case class FleetShip(shipType: ShipType, amount: Int)

case class PlanetFleet(playerPlanet: PlayerPlanet, fleet: Map[ShipType, Int])
case class EscapeConfig(target: Coordinates, interval: FiniteDuration, minEscapeTime: FiniteDuration, escapeTimeThreshold: FiniteDuration)
case class FlyAndReturnConfig(
    from: PlanetId,
    to: PlanetId,
    isOn: Boolean,
    safeBuffer: FiniteDuration,
    randomUpperLimit: FiniteDuration,
    remainDeuterAmount: Int,
    speeds: List[FleetSpeed]
)
