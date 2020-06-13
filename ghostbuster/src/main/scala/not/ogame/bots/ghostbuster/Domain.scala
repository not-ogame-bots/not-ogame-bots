package not.ogame.bots.ghostbuster

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots._

import scala.concurrent.duration.FiniteDuration

sealed trait Wish
object Wish {
  case class BuildSupply(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, planetId: String) extends Wish

  case class BuildFacility(facilityBuilding: FacilityBuilding, level: Int Refined Positive, planetId: String) extends Wish

  case class BuildShip(shipType: ShipType, planetId: String, amount: Int Refined Positive) extends Wish

  case class SmartSupplyBuilder(
      metalLevel: Int Refined Positive,
      crystalLevel: Int Refined Positive,
      deuterLevel: Int Refined Positive,
      planetId: String
  ) extends Wish

  case class Research(technology: Technology, level: Int Refined Positive, planetId: String) extends Wish
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
    deuterThreshold: Int,
    eligiblePlanets: List[PlanetId],
    builder: Boolean,
    maxWaitTime: FiniteDuration,
    maxBuildingTime: FiniteDuration
)
case class ExpeditionConfig(
    ships: List[FleetShip],
    isOn: Boolean,
    maxNumberOfExpeditions: Int,
    deuterThreshold: Int,
    eligiblePlanets: List[PlanetId]
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
    remainDeuterAmount: Int
)
