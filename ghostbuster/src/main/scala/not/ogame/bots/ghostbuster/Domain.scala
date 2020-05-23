package not.ogame.bots.ghostbuster

import java.time.Instant
import java.util.UUID

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots._

sealed trait Action {
  def uuid: UUID
  def executionTime: Instant
}

case class Response(value: Any, uuid: UUID)

object Action {
  case class BuildSupply(
      suppliesBuilding: SuppliesBuilding,
      level: Int Refined Positive,
      executionTime: Instant,
      planetId: String,
      uuid: UUID = UUID.randomUUID()
  ) extends Action {
    def defer(any: Any): Instant = any.asInstanceOf[Instant]
    def response(value: Instant): Response = Response(value, uuid)
  }

  case class BuildFacility(
      suppliesBuilding: FacilityBuilding,
      level: Int Refined Positive,
      executionTime: Instant,
      planetId: String,
      uuid: UUID = UUID.randomUUID()
  ) extends Action {
    def defer(any: Any): FacilitiesBuildingLevels = any.asInstanceOf[FacilitiesBuildingLevels]
    def response(value: FacilitiesBuildingLevels): Response = Response(value, uuid)
  }

  case class RefreshSupplyAndFacilityPage(executionTime: Instant, planetId: String, uuid: UUID = UUID.randomUUID()) extends Action { //TODO remove
    def defer(any: Any): Any = any.asInstanceOf[Any]
    def response(value: Any): Response = Response(value, uuid)
  }

  case class ReadSupplyPage(executionTime: Instant, planetId: String, uuid: UUID = UUID.randomUUID()) extends Action {
    def defer(any: Any): SuppliesPageData = any.asInstanceOf[SuppliesPageData]
    def response(value: SuppliesPageData): Response = Response(value, uuid)
  }

  case class RefreshFleetOnPlanetStatus(executionTime: Instant, playerPlanet: PlayerPlanet, uuid: UUID = UUID.randomUUID()) extends Action {
    def defer(any: Any): PlanetFleet = any.asInstanceOf[PlanetFleet]
    def response(value: PlanetFleet): Response = Response(value, uuid)
  }

  case class BuildShip(amount: Int, shipType: ShipType, executionTime: Instant, planetId: String, uuid: UUID = UUID.randomUUID())
      extends Action {
    def defer(any: Any): SuppliesPageData = any.asInstanceOf[SuppliesPageData]
    def response(value: SuppliesPageData): Response = Response(value, uuid)
  }

  case class DumpActivity(executionTime: Instant, planets: List[String], uuid: UUID = UUID.randomUUID()) extends Action {
    def defer(any: Any): Unit = any.asInstanceOf[Unit]
    def response(value: Unit): Response = Response(value, uuid)
  }

  case class SendFleet(executionTime: Instant, sendFleetRequest: SendFleetRequest, uuid: UUID = UUID.randomUUID()) extends Action {
    def defer(any: Any): Instant = any.asInstanceOf[Instant]
    def response(value: Instant): Response = Response(value, uuid)
  }

  case class GetAirFleet(executionTime: Instant, uuid: UUID = UUID.randomUUID()) extends Action {
    def defer(any: Any): List[Fleet] = any.asInstanceOf[List[Fleet]]
    def response(value: List[Fleet]): Response = Response(value, uuid)
  }

  case class ReadPlanets(executionTime: Instant, uuid: UUID = UUID.randomUUID()) extends Action {
    def defer(any: Any): List[PlayerPlanet] = any.asInstanceOf[List[PlayerPlanet]]
    def response(value: List[PlayerPlanet]): Response = Response(value, uuid)
  }
}

sealed trait Wish
object Wish {
  case class BuildSupply(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, planetId: String) extends Wish

  case class BuildFacility(facility: FacilityBuilding, level: Int Refined Positive, planetId: String) extends Wish

  case class BuildShip(shipType: ShipType, planetId: String, amount: Int Refined Positive) extends Wish
}

case class PlanetState(
    id: String,
    coords: Coordinates,
    suppliesPage: SuppliesPageData,
    facilityBuildingLevels: FacilitiesBuildingLevels,
    fleetOnPlanet: Map[ShipType, Int]
) {
  def buildingInProgress: Boolean = suppliesPage.currentBuildingProgress.isDefined
  def shipInProgress: Boolean = suppliesPage.currentShipyardProgress.isDefined
  def isBusy: Boolean = buildingInProgress || shipInProgress
  def isIdle: Boolean = !isBusy
}

//sealed trait State {
//  def scheduledTasks: List[Action]
//}
//
//object State {
//  case class LoggedOut(scheduledTasks: List[Action]) extends State
//
//  case class LoggedIn(scheduledTasks: List[Action], planets: List[PlanetState], fleets: List[Fleet]) extends State
//}

case class BotConfig(
    wishlist: List[Wish],
    buildMtUpToCapacity: Boolean,
    useWishlist: Boolean,
    activityFaker: Boolean,
    allowWaiting: Boolean
)
case class PlanetFleet(playerPlanet: PlayerPlanet, fleet: Map[ShipType, Int])
