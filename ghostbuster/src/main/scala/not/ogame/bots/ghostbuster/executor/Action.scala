package not.ogame.bots.ghostbuster.executor

import java.time.Instant
import java.util.UUID

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots.ghostbuster.PlanetFleet
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
  ) extends Action[FacilityPageData]

  case class ReadSupplyPage(executionTime: Instant, planetId: String, uuid: UUID = UUID.randomUUID()) extends Action[SuppliesPageData]

  case class RefreshFleetOnPlanetStatus(executionTime: Instant, playerPlanet: PlayerPlanet, uuid: UUID = UUID.randomUUID())
      extends Action[PlanetFleet]

  case class BuildShip(amount: Int, shipType: ShipType, executionTime: Instant, planetId: String, uuid: UUID = UUID.randomUUID())
      extends Action[SuppliesPageData]

  case class SendFleet(executionTime: Instant, sendFleetRequest: SendFleetRequest, uuid: UUID = UUID.randomUUID()) extends Action[Instant]

  case class GetAirFleet(executionTime: Instant, uuid: UUID = UUID.randomUUID()) extends Action[List[Fleet]]

  case class ReadPlanets(executionTime: Instant, uuid: UUID = UUID.randomUUID()) extends Action[List[PlayerPlanet]]
}
