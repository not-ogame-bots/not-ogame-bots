package not.ogame.bots.ghostbuster.notifications

import java.time.ZonedDateTime

import not.ogame.bots._

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
  case class SendFleet(sendFleetRequest: SendFleetRequest) extends Notification
  case class ReturnFleet(fleetId: FleetId) extends Notification
  case class ReadAllFleets(fleets: List[Fleet]) extends Notification
  case class ReadPlanets(value: List[PlayerPlanet]) extends Notification
  case class ReturnFleetAction(fleetId: FleetId, value: ZonedDateTime) extends Notification
  case class ReadMyFleetAction(myFleetPageData: MyFleetPageData) extends Notification
  case class FleetArrived(coordinates: Coordinates) extends Notification
  case class ExpeditionPhaseOneCompleted(from: Coordinates, fleetId: FleetId) extends Notification
  case class ExpeditionPhaseTwoCompleted(from: Coordinates, fleetId: FleetId) extends Notification
  case class ExpeditionReturned(from: Coordinates, fleetId: FleetId) extends Notification
  case class ExpeditionDestroyed(from: Coordinates, fleetId: FleetId) extends Notification
}
