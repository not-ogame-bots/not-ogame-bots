package not.ogame.bots.ghostbuster

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots._

sealed trait Task {
  def executeAfter: Instant
}
object Task {
  case class BuildSupply(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, executeAfter: Instant) extends Task

  case class BuildFacility(suppliesBuilding: FacilityBuilding, level: Int Refined Positive, executeAfter: Instant) extends Task

  case class RefreshSupplyAndFacilityPage(executeAfter: Instant) extends Task

  case class RefreshFleetOnPlanetStatus(shipType: ShipType, executeAfter: Instant) extends Task

  case class BuildShip(amount: Int, shipType: ShipType, executeAfter: Instant) extends Task

  def buildSupply(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, executeAfter: Instant): Task = {
    BuildSupply(suppliesBuilding, level, executeAfter)
  }

  def refreshSupplyPage(executeAfter: Instant): Task = Task.RefreshSupplyAndFacilityPage(executeAfter)

  def refreshFleetOnPlanetStatus(shipType: ShipType, executeAfter: Instant): Task = Task.RefreshFleetOnPlanetStatus(shipType, executeAfter)
}

sealed trait Wish
object Wish {
  case class BuildSupply(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive) extends Wish

  def buildSupply(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive): Wish = BuildSupply(suppliesBuilding, level)

  case class BuildFacility(facility: FacilityBuilding, level: Int Refined Positive) extends Wish

  def buildFacility(facility: FacilityBuilding, level: Int Refined Positive): Wish = BuildFacility(facility, level)
}

sealed trait PlanetState {
  def scheduledTasks: List[Task]
}

object PlanetState {
  case class LoggedOut(scheduledTasks: List[Task]) extends PlanetState

  case class LoggedIn(
      suppliesPage: SuppliesPageData,
      scheduledTasks: List[Task],
      facilityBuildingLevels: FacilitiesBuildingLevels,
      fleetOnPlanet: Map[ShipType, Int]
  ) extends PlanetState

  def loggedIn(
      suppliesPage: SuppliesPageData,
      scheduledTasks: List[Task],
      facilityBuildingLevels: FacilitiesBuildingLevels
  ): PlanetState = {
    PlanetState.LoggedIn(suppliesPage, scheduledTasks, facilityBuildingLevels, Map.empty)
  }
}

case class BotConfig(wishlist: List[Wish], buildMtUpToCapacity: Boolean, useWishlist: Boolean)
