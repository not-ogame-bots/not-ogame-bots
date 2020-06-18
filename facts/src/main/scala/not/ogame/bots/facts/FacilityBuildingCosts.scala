package not.ogame.bots.facts

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV
import not.ogame.bots.{FacilityBuilding, Resources}

object FacilityBuildingCosts {
  def buildingCost(facilityBuilding: FacilityBuilding, level: Int): Resources = {
    buildingCost(facilityBuilding, refineVUnsafe[Positive, Int](level))
  }

  def buildingCost(facilityBuilding: FacilityBuilding, level: Int Refined Positive): Resources = {
    facilityBuilding match {
      case FacilityBuilding.RoboticsFactory => fromBaseCostPowerOf2(Resources(400, 120, 200), level.value)
      case FacilityBuilding.Shipyard        => fromBaseCostPowerOf2(Resources(400, 200, 100), level.value)
      case FacilityBuilding.ResearchLab     => fromBaseCostPowerOf2(Resources(200, 400, 200), level.value)
      case FacilityBuilding.NaniteFactory   => fromBaseCostPowerOf2(Resources(1_000_000, 500_000, 100_000), level.value)
    }
  }

  private def fromBaseCostPowerOf2(baseCost: Resources, level: Int): Resources = {
    Resources(
      metal = (baseCost.metal * 2.0.pow(level - 1.0)).toInt,
      crystal = (baseCost.crystal * 2.0.pow(level - 1.0)).toInt,
      deuterium = (baseCost.deuterium * 2.0.pow(level - 1.0)).toInt
    )
  }

  private def refineVUnsafe[P, V](v: V)(implicit ev: Validate[V, P]): Refined[V, P] =
    refineV[P](v).fold(s => throw new IllegalArgumentException(s), identity)
}
