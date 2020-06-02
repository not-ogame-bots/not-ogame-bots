package not.ogame.bots.ghostbuster.executor

import java.time.ZonedDateTime
import java.util.UUID

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots._
import not.ogame.bots.ghostbuster.PlanetFleet

sealed trait Action[T] {
  def uuid: UUID
  def defer(any: Any): T = any.asInstanceOf[T]
  def success(value: T): Response = Response.Success(value, uuid)
  def failure(throwable: Throwable): Response = Response.Failure(throwable, uuid)
}

sealed trait Response {
  def uuid: UUID
}
object Response {
  case class Success(value: Any, uuid: UUID) extends Response
  case class Failure(ex: Throwable, uuid: UUID) extends Response
}

object Action {
  case class BuildSupply(
      suppliesBuilding: SuppliesBuilding,
      level: Int Refined Positive,
      playerPlanet: PlayerPlanet,
      uuid: UUID = UUID.randomUUID()
  ) extends Action[ZonedDateTime]

  case class BuildFacility(
      facilityBuilding: FacilityBuilding,
      level: Int Refined Positive,
      playerPlanet: PlayerPlanet,
      uuid: UUID = UUID.randomUUID()
  ) extends Action[ZonedDateTime]

  case class ReadSupplyPage(playerPlanet: PlayerPlanet, uuid: UUID = UUID.randomUUID()) extends Action[SuppliesPageData]
  case class ReadFacilityPage(playerPlanet: PlayerPlanet, uuid: UUID = UUID.randomUUID()) extends Action[FacilityPageData]

  case class RefreshFleetOnPlanetStatus(playerPlanet: PlayerPlanet, uuid: UUID = UUID.randomUUID()) extends Action[PlanetFleet]

  case class BuildShip(amount: Int, shipType: ShipType, playerPlanet: PlayerPlanet, uuid: UUID = UUID.randomUUID())
      extends Action[SuppliesPageData]

  case class SendFleet(sendFleetRequest: SendFleetRequest, uuid: UUID = UUID.randomUUID()) extends Action[ZonedDateTime]

  case class GetAirFleet(uuid: UUID = UUID.randomUUID()) extends Action[List[Fleet]]

  case class ReadPlanets(uuid: UUID = UUID.randomUUID()) extends Action[List[PlayerPlanet]]

  case class ReturnFleetAction(fleetId: FleetId, uuid: UUID = UUID.randomUUID()) extends Action[ZonedDateTime]

  case class ReadMyFleetAction(uuid: UUID = UUID.randomUUID()) extends Action[List[MyFleet]]
}
