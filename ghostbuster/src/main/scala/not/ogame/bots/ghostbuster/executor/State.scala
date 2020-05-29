package not.ogame.bots.ghostbuster.executor

import java.time.ZonedDateTime

import not.ogame.bots._

case class State(
    lastTimestamp: Option[ZonedDateTime],
    lastError: Option[(ZonedDateTime, String)],
    enemyFleets: List[Fleet],
    summaryFleetOnPlanets: Map[ShipType, Int],
    summaryResourcesOnPlanets: Option[Resources],
    airFleets: List[Fleet],
    planets: Map[Coordinates, PlanetState]
)

case class PlanetState(
    currentResources: Option[Resources],
    currentProduction: Option[Resources],
    currentCapacity: Option[Resources],
    suppliesLevels: Option[SuppliesBuildingLevels],
    facilitiesBuildingLevels: Option[FacilitiesBuildingLevels],
    currentBuildingProgress: Option[BuildingProgress],
    currentShipyardProgress: Option[BuildingProgress],
    fleet: Option[Map[ShipType, Int]]
)
object PlanetState {
  val Empty: PlanetState = PlanetState(None, None, None, None, None, None, None, None)
}

object State {
  val Empty: State = State(None, None, List.empty, Map.empty, None, List.empty, Map.empty)
}
