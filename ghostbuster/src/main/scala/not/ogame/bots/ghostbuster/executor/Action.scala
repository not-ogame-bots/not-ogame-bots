package not.ogame.bots.ghostbuster.executor

import java.time.ZonedDateTime
import java.util.UUID

import cats.effect.concurrent.MVar
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.ghostbuster.PlanetFleet

case class Request[T](action: Task[T], response: MVar[Task, Response[T]])
object Request {
  def apply[T](action: Task[T]): Task[Request[T]] = {
    MVar.empty[Task, Response[T]].map(new Request[T](action, _))
  }
}

sealed trait Action[T]

sealed trait Response[T]
object Response {
  case class Success[T](value: T) extends Response[T]
  case class Failure[T](ex: Throwable) extends Response[T]

  def success[T](t: T): Response[T] = Success(t)
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

sealed trait Notification
object Notification {
  case class Login() extends Notification
  case class SupplyBuilt(value: ZonedDateTime) extends Notification
  case class FacilityBuilt(value: ZonedDateTime) extends Notification
  case class SuppliesPageDateRefreshed(value: SuppliesPageData, playerPlanet: PlayerPlanet) extends Notification
  case class FacilityPageDataRefreshed(value: FacilityPageData, playerPlanet: PlayerPlanet) extends Notification
  case class Failure(ex: Throwable) extends Notification
  case class FleetOnPlanetRefreshed(value: FleetPageData, playerPlanet: PlayerPlanet) extends Notification
  case class ShipBuilt(shipType: ShipType, amount: Int, playerPlanet: PlayerPlanet, time: Option[ZonedDateTime]) extends Notification
  case class FleetSent(sendFleetRequest: SendFleetRequest, value: ZonedDateTime) extends Notification
  case class GetAirFleet(value: List[Fleet]) extends Notification
  case class ReadPlanets(value: List[PlayerPlanet]) extends Notification
  case class ReturnFleetAction(fleetId: FleetId, value: ZonedDateTime) extends Notification
  case class ReadMyFleetAction(value: List[MyFleet]) extends Notification
}
