package not.ogame.bots.ghostbuster

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots._

sealed trait Wish
object Wish {
  case class BuildSupply(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, planetId: String) extends Wish

  case class BuildFacility(facility: FacilityBuilding, level: Int Refined Positive, planetId: String) extends Wish

  case class BuildShip(shipType: ShipType, planetId: String, amount: Int Refined Positive) extends Wish

  case class SmartSupplyBuilder(
      metalLevel: Int Refined Positive,
      crystalLevel: Int Refined Positive,
      deuterLevel: Int Refined Positive,
      planetId: String
  ) extends Wish
}

case class BotConfig(
    wishlist: List[Wish],
    fs: FsConfig
)

case class FsConfig(ships: List[FsShip])
case class FsShip(shipType: ShipType, amount: Int)

case class PlanetFleet(playerPlanet: PlayerPlanet, fleet: Map[ShipType, Int])
