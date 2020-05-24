package not.ogame.bots

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineMV

package object ghostbuster {
//  def createStartingBuildings: Map[SuppliesBuilding, Int Refined NonNegative] = {
//    SuppliesBuilding.values.map(_ -> refineMV[NonNegative](0)).toMap
//  }
//
//  def createFacilityBuildings: FacilitiesBuildingLevels = {
//    FacilitiesBuildingLevels(FacilityBuilding.values.map(_ -> refineMV[NonNegative](0)).toMap)
//  }
//  val bigCapacity: Resources = Resources(10000, 10000, 10000, 0)
//
//  val planetId1 = "33653280"
//  val planet1Coords: Coordinates = Coordinates(1, 1, 1)
//  val planet2Coords: Coordinates = Coordinates(2, 2, 2)
//  val now: Instant = Instant.now()
//
//  def createPlanetState(
//      suppliesPageData: SuppliesPageData = createSuppliesPage(),
//      facilitiesBuildingLevels: FacilitiesBuildingLevels = createFacilityBuildings,
//      fleet: Map[ShipType, Int] = Map.empty,
//      planetId: String = planetId1
//  ): PlanetState = {
//    PlanetState(
//      planetId,
//      planet1Coords,
//      suppliesPageData,
//      facilitiesBuildingLevels,
//      fleet
//    )
//  }
//
//  def createSuppliesPage(
//      resources: Resources = Resources.Zero,
//      production: Resources = Resources(1, 1, 1),
//      capacity: Resources = bigCapacity,
//      suppliesBuildingLevels: SuppliesBuildingLevels = SuppliesBuildingLevels(createStartingBuildings),
//      currentBuildinProgress: Option[BuildingProgress] = None,
//      currentShipyardProgress: Option[BuildingProgress] = None
//  ): SuppliesPageData = {
//    SuppliesPageData(
//      now,
//      resources,
//      production,
//      capacity,
//      suppliesBuildingLevels,
//      currentBuildinProgress,
//      currentShipyardProgress
//    )
//  }
}
