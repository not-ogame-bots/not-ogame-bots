package not.ogame.bots.ghostbuster.executor

import java.time.ZonedDateTime

import not.ogame.bots._

case class State(
    lastTimestamp: Option[ZonedDateTime],
    enemyFleets: List[Fleet],
    planets: Map[Coordinates, PlanetState],
    airFleets: List[Fleet]
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
  val Empty: State = State(None, List.empty, Map.empty, List.empty)
}
