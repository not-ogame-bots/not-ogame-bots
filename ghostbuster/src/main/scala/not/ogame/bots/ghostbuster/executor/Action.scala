package not.ogame.bots.ghostbuster.executor

import java.time.ZonedDateTime
import java.util.UUID

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots.ghostbuster.PlanetFleet
import not.ogame.bots._

sealed trait Action[T] {
  def uuid: UUID
  def executionTime: ZonedDateTime
  def defer(any: Any): T = any.asInstanceOf[T]
  def success(value: T): Response = Response.Success(value, uuid)
  def failure(): Response = Response.Failure(uuid)
}

sealed trait Response {
  def uuid: UUID
}
object Response {
  case class Success(value: Any, uuid: UUID) extends Response
  case class Failure(uuid: UUID) extends Response
}

object Action {
  case class BuildSupply(
      suppliesBuilding: SuppliesBuilding,
      level: Int Refined Positive,
      executionTime: ZonedDateTime,
      planetId: String,
      uuid: UUID = UUID.randomUUID()
  ) extends Action[ZonedDateTime]

  case class BuildFacility(
      suppliesBuilding: FacilityBuilding,
      level: Int Refined Positive,
      executionTime: ZonedDateTime,
      planetId: String,
      uuid: UUID = UUID.randomUUID()
  ) extends Action[FacilityPageData]

  case class ReadSupplyPage(executionTime: ZonedDateTime, planetId: String, uuid: UUID = UUID.randomUUID()) extends Action[SuppliesPageData]

  case class RefreshFleetOnPlanetStatus(executionTime: ZonedDateTime, playerPlanet: PlayerPlanet, uuid: UUID = UUID.randomUUID())
      extends Action[PlanetFleet]

  case class BuildShip(amount: Int, shipType: ShipType, executionTime: ZonedDateTime, planetId: String, uuid: UUID = UUID.randomUUID())
      extends Action[SuppliesPageData]

  case class SendFleet(executionTime: ZonedDateTime, sendFleetRequest: SendFleetRequest, uuid: UUID = UUID.randomUUID())
      extends Action[ZonedDateTime]

  case class GetAirFleet(executionTime: ZonedDateTime, uuid: UUID = UUID.randomUUID()) extends Action[List[Fleet]]

  case class ReadPlanets(executionTime: ZonedDateTime, uuid: UUID = UUID.randomUUID()) extends Action[List[PlayerPlanet]]
}
