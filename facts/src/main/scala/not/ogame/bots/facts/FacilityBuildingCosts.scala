package not.ogame.bots.facts

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots.{FacilityBuilding, Resources}

object FacilityBuildingCosts {
  def buildingCost(facilityBuilding: FacilityBuilding, level: Int Refined Positive): Resources = {
    facilityBuilding match {
      case FacilityBuilding.RoboticsFactory => roboticsFactoryCost(level.value)
      case FacilityBuilding.Shipyard        => shipyardCost(level.value)
      case FacilityBuilding.ResearchLab     => researchLabCost(level.value)
      case FacilityBuilding.NaniteFactory   => ???
    }
  }

  private def roboticsFactoryCost(level: Int): Resources = {
    Resources(
      metal = (400.0 * 2.0.pow(level - 1.0)).toInt,
      crystal = (120.0 * 2.0.pow(level - 1.0)).toInt,
      deuterium = (200.0 * 2.0.pow(level - 1.0)).toInt
    )
  }

  private def shipyardCost(level: Int): Resources = {
    Resources(
      metal = (400.0 * 2.0.pow(level - 1.0)).toInt,
      crystal = (200.0 * 2.0.pow(level - 1.0)).toInt,
      deuterium = (100.0 * 2.0.pow(level - 1.0)).toInt
    )
  }

  private def researchLabCost(level: Int): Resources = {
    Resources(
      metal = (200.0 * 2.0.pow(level - 1.0)).toInt,
      crystal = (400.0 * 2.0.pow(level - 1.0)).toInt,
      deuterium = (200.0 * 2.0.pow(level - 1.0)).toInt
    )
  }
}
