package not.ogame.bots.facts

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots.{Resources, SuppliesBuilding}

object SuppliesBuildingCosts {
  def buildingCost(buildingType: SuppliesBuilding, level: Int Refined Positive): Resources = {
    buildingType match {
      case SuppliesBuilding.MetalMine            => metalMineCost(level.value)
      case SuppliesBuilding.CrystalMine          => crystalMineCost(level.value)
      case SuppliesBuilding.DeuteriumSynthesizer => deuteriumSynthesiserCost(level.value)
      case SuppliesBuilding.SolarPlant           => powerPlantCost(level.value)
      case SuppliesBuilding.MetalStorage         => fromBaseCostPowerOf2(Resources(1000, 0, 0), level.value)
      case SuppliesBuilding.CrystalStorage       => fromBaseCostPowerOf2(Resources(1000, 500, 0), level.value)
      case SuppliesBuilding.DeuteriumStorage     => fromBaseCostPowerOf2(Resources(1000, 1000, 0), level.value)
    }
  }

  private def metalMineCost(level: Int): Resources = {
    Resources(metal = (60.0 * 1.5.pow(level - 1.0)).toInt, crystal = (15.0 * 1.5.pow(level - 1.0)).toInt, deuterium = 0)
  }

  private def crystalMineCost(level: Int): Resources = {
    Resources(metal = (48.0 * 1.6.pow(level - 1.0)).toInt, crystal = (24.0 * 1.6.pow(level - 1.0)).toInt, deuterium = 0)
  }

  private def deuteriumSynthesiserCost(level: Int): Resources = {
    Resources(metal = (225.0 * 1.5.pow(level - 1.0)).toInt, crystal = (75.0 * 1.5.pow(level - 1.0)).toInt, deuterium = 0)
  }

  private def powerPlantCost(level: Int): Resources = {
    Resources(metal = (75.0 * 1.5.pow(level - 1.0)).toInt, crystal = (30.0 * 1.5.pow(level - 1.0)).toInt, deuterium = 0)
  }

  private def fromBaseCostPowerOf2(baseCost: Resources, level: Int): Resources = {
    Resources(
      metal = (baseCost.metal * 2.0.pow(level - 1.0)).toInt,
      crystal = (baseCost.crystal * 2.0.pow(level - 1.0)).toInt,
      deuterium = (baseCost.deuterium * 2.0.pow(level - 1.0)).toInt
    )
  }
}
