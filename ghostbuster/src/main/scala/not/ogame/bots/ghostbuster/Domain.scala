package not.ogame.bots.ghostbuster

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots._

sealed trait Wish
object Wish {
  case class BuildSupply(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, planetId: String) extends Wish

  case class BuildFacility(facility: FacilityBuilding, level: Int Refined Positive, planetId: String) extends Wish

  case class BuildShip(shipType: ShipType, planetId: String, amount: Int Refined Positive) extends Wish
}

case class BotConfig(
    wishlist: List[Wish],
    buildMtUpToCapacity: Boolean,
    useWishlist: Boolean,
    activityFaker: Boolean,
    allowWaiting: Boolean
)
case class PlanetFleet(playerPlanet: PlayerPlanet, fleet: Map[ShipType, Int])
