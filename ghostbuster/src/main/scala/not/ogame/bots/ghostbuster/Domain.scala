package not.ogame.bots.ghostbuster

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots._

sealed trait Task {
  def executeAfter: Instant
}
object Task {
  case class BuildSupply(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, executeAfter: Instant, planetId: String)
      extends Task

  case class BuildFacility(suppliesBuilding: FacilityBuilding, level: Int Refined Positive, executeAfter: Instant, planetId: String)
      extends Task

  case class RefreshSupplyAndFacilityPage(executeAfter: Instant, planetId: String) extends Task

  case class RefreshFleetOnPlanetStatus(shipType: ShipType, executeAfter: Instant, planetId: String) extends Task

  case class BuildShip(amount: Int, shipType: ShipType, executeAfter: Instant, planetId: String) extends Task

  case class DumpActivity(executeAfter: Instant, planets: List[String]) extends Task
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

sealed trait State {
  def scheduledTasks: List[Task]
}

object State {
  case class LoggedOut(scheduledTasks: List[Task]) extends State

  case class LoggedIn(scheduledTasks: List[Task], planets: List[PlanetState], fleets: List[Fleet]) extends State
}

case class BotConfig(wishlist: List[Wish], buildMtUpToCapacity: Boolean, useWishlist: Boolean, activityFaker: Boolean)
