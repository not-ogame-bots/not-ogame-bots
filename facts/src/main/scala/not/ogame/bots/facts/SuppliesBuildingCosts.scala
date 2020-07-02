package not.ogame.bots.facts

import not.ogame.bots.{Resources, SuppliesBuilding}

object SuppliesBuildingCosts {
  def buildingCost(buildingType: SuppliesBuilding, level: Int): Resources = {
    buildingType match {
      case SuppliesBuilding.MetalMine            => metalMineCost(level)
      case SuppliesBuilding.CrystalMine          => crystalMineCost(level)
      case SuppliesBuilding.DeuteriumSynthesizer => deuteriumSynthesiserCost(level)
      case SuppliesBuilding.SolarPlant           => powerPlantCost(level)
      case SuppliesBuilding.FusionPlant          => fusionPlantCost(level)
      case SuppliesBuilding.MetalStorage         => fromBaseCostPowerOf2(Resources(1000, 0, 0), level)
      case SuppliesBuilding.CrystalStorage       => fromBaseCostPowerOf2(Resources(1000, 500, 0), level)
      case SuppliesBuilding.DeuteriumStorage     => fromBaseCostPowerOf2(Resources(1000, 1000, 0), level)
    }
  }

  private def metalMineCost(level: Int): Resources = {
    calculateCostFromBaseAndPower(Resources(60, 15, 0), 1.5, level)
  }

  private def crystalMineCost(level: Int): Resources = {
    calculateCostFromBaseAndPower(Resources(48, 24, 0), 1.6, level)
  }

  private def deuteriumSynthesiserCost(level: Int): Resources = {
    calculateCostFromBaseAndPower(Resources(225, 75, 0), 1.5, level)
  }

  private def powerPlantCost(level: Int): Resources = {
    calculateCostFromBaseAndPower(Resources(75, 30, 0), 1.5, level)
  }

  private def fusionPlantCost(level: Int): Resources = {
    calculateCostFromBaseAndPower(Resources(900, 360, 180), 1.8, level)
  }

  private def fromBaseCostPowerOf2(baseCost: Resources, level: Int): Resources = {
    calculateCostFromBaseAndPower(baseCost, 2.0, level)
  }

  private def calculateCostFromBaseAndPower(baseCosts: Resources, power: Double, level: Int) = {
    baseCosts.multiply(power.pow(level - 1.0))
  }
}
