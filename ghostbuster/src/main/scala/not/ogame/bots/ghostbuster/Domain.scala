package not.ogame.bots.ghostbuster

import java.time.Instant
import java.util.UUID

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots._

sealed trait Action[T] {
  def uuid: UUID
  def executionTime: Instant
  def defer(any: Any): T = any.asInstanceOf[T]
  def response(value: T): Response = Response(value, uuid)
}

case class Response(value: Any, uuid: UUID)

object Action {
  case class BuildSupply(
      suppliesBuilding: SuppliesBuilding,
      level: Int Refined Positive,
      executionTime: Instant,
      planetId: String,
      uuid: UUID = UUID.randomUUID()
  ) extends Action[Instant]

  case class BuildFacility(
      suppliesBuilding: FacilityBuilding,
      level: Int Refined Positive,
      executionTime: Instant,
      planetId: String,
      uuid: UUID = UUID.randomUUID()
  ) extends Action[FacilitiesBuildingLevels]

  case class RefreshSupplyAndFacilityPage(executionTime: Instant, planetId: String, uuid: UUID = UUID.randomUUID()) extends Action[Any]

  case class ReadSupplyPage(executionTime: Instant, planetId: String, uuid: UUID = UUID.randomUUID()) extends Action[SuppliesPageData]

  case class RefreshFleetOnPlanetStatus(executionTime: Instant, playerPlanet: PlayerPlanet, uuid: UUID = UUID.randomUUID())
      extends Action[PlanetFleet]

  case class BuildShip(amount: Int, shipType: ShipType, executionTime: Instant, planetId: String, uuid: UUID = UUID.randomUUID())
      extends Action[SuppliesPageData]

  case class DumpActivity(executionTime: Instant, planets: List[String], uuid: UUID = UUID.randomUUID()) extends Action[Unit]

  case class SendFleet(executionTime: Instant, sendFleetRequest: SendFleetRequest, uuid: UUID = UUID.randomUUID()) extends Action[Instant]

  case class GetAirFleet(executionTime: Instant, uuid: UUID = UUID.randomUUID()) extends Action[List[Fleet]]

  case class ReadPlanets(executionTime: Instant, uuid: UUID = UUID.randomUUID()) extends Action[List[PlayerPlanet]]
}

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
