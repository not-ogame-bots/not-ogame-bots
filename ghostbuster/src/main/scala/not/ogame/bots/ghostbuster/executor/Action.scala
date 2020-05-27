package not.ogame.bots.ghostbuster.executor

import java.time.ZonedDateTime

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots._
import not.ogame.bots.ghostbuster.PlanetFleet

sealed trait Action[T] {
  def defer(any: Any): T = any.asInstanceOf[T]
  def success(value: T): Response = Response.Success(value)
  def failure(throwable: Throwable): Response = Response.Failure(throwable)
}

sealed trait Response
object Response {
  case class Success(value: Any) extends Response
  case class Failure(ex: Throwable) extends Response
}

object Action {
  case class BuildSupply(
      suppliesBuilding: SuppliesBuilding,
      level: Int Refined Positive,
      playerPlanet: PlayerPlanet
  ) extends Action[ZonedDateTime]

  case class BuildFacility(
      facilityBuilding: FacilityBuilding,
      level: Int Refined Positive,
      playerPlanet: PlayerPlanet
  ) extends Action[ZonedDateTime]

  case class ReadSupplyPage(playerPlanet: PlayerPlanet) extends Action[SuppliesPageData]
  case class ReadFacilityPage(playerPlanet: PlayerPlanet) extends Action[FacilityPageData]

  case class RefreshFleetOnPlanetStatus(playerPlanet: PlayerPlanet) extends Action[PlanetFleet]

  case class BuildShip(amount: Int, shipType: ShipType, playerPlanet: PlayerPlanet) extends Action[SuppliesPageData]

  case class SendFleet(sendFleetRequest: SendFleetRequest) extends Action[ZonedDateTime]

  case class GetAirFleet() extends Action[List[Fleet]]

  case class ReadPlanets() extends Action[List[PlayerPlanet]]
}
