package not.ogame.bots.ghostbuster.executor

import java.time.ZonedDateTime

import cats.effect.concurrent.MVar
import monix.eval.Task
import not.ogame.bots._

case class Request[T](action: Task[T], response: MVar[Task, Response[T]])
object Request {
  def apply[T](action: Task[T]): Task[Request[T]] = {
    MVar.empty[Task, Response[T]].map(new Request[T](action, _))
  }
}

sealed trait Response[T]
object Response {
  case class Success[T](value: T) extends Response[T]
  case class Failure[T](ex: Throwable) extends Response[T]

  def success[T](t: T): Response[T] = Success(t)
}

sealed trait Notification
object Notification {
  case class Login() extends Notification
  case class SupplyBuilt(planetId: PlanetId, suppliesBuilding: SuppliesBuilding) extends Notification
  case class FacilityBuilt(planetId: PlanetId, facilityBuilding: FacilityBuilding) extends Notification
  case class ResearchStarted(planetId: PlanetId, technology: Technology) extends Notification
  case class SuppliesPageDateRefreshed(value: SuppliesPageData, planetId: PlanetId) extends Notification
  case class FacilityPageDataRefreshed(value: FacilityPageData, planetId: PlanetId) extends Notification
  case class TechnologyPageDataRefreshed(value: TechnologyPageData, planetId: PlanetId) extends Notification
  case class Failure(ex: Throwable) extends Notification
  case class FleetOnPlanetRefreshed(value: FleetPageData, planetId: PlanetId) extends Notification
  case class ShipBuilt(shipType: ShipType, amount: Int, planetId: PlanetId) extends Notification
  case class FleetSent(sendFleetRequest: SendFleetRequest) extends Notification
  case class FleetReturned(fleetId: FleetId) extends Notification
  case class ReadAllFleets(value: List[Fleet]) extends Notification
  case class ReadPlanets(value: List[PlayerPlanet]) extends Notification
  case class ReturnFleetAction(fleetId: FleetId, value: ZonedDateTime) extends Notification
  case class ReadMyFleetAction(myFleetPageData: MyFleetPageData) extends Notification
}
