package not.ogame.bots

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineMV

package object ghostbuster {
  def createStartingBuildings: Map[SuppliesBuilding, Int Refined NonNegative] = {
    SuppliesBuilding.values.map(_ -> refineMV[NonNegative](0)).toMap
  }

  def createFacilityBuildings: FacilitiesBuildingLevels = {
    FacilitiesBuildingLevels(FacilityBuilding.values.map(_ -> refineMV[NonNegative](0)).toMap)
  }
  val bigCapacity: Resources = Resources(10000, 10000, 10000, 0)
}
