package not.ogame.bots.ordon

import not.ogame.bots.FacilityBuilding.{ResearchLab, RoboticsFactory, Shipyard}
import not.ogame.bots.ShipType.{ColonyShip, SmallCargoShip}
import not.ogame.bots.SuppliesBuilding.{CrystalMine, DeuteriumSynthesizer, MetalMine, SolarPlant}
import not.ogame.bots.Technology._
import not.ogame.bots.facts.{EnergyConsumption, FacilityBuildingCosts, Production, ShipCosts, SuppliesBuildingCosts, TechnologyCosts}
import not.ogame.bots.{Resources, SuppliesBuilding}

class TestStrategiesSpec extends munit.FunSuite {
  //  test("Test strategies") {
  //    val ogame = Ogame(
  //      resources = Resources(120, 200, 75),
  //      buildings = Map(MetalMine -> 6, CrystalMine -> 4, DeuteriumSynthesizer -> 3, SolarPlant -> 6)
  //    ).buildE(CrystalMine) //5
  //      .buildE(MetalMine) //7
  //      .buildE(CrystalMine) //6
  //      .buildE(MetalMine) //8
  //      .buildE(CrystalMine) //7
  //      .buildE(DeuteriumSynthesizer) //4
  //      .buildE(DeuteriumSynthesizer) //5
  //      .buildE(CrystalMine) //8
  //      .advanceTime(costForAstrophysics())
  //    println(ogame)
  //    println(ogame.timeInSeconds.toDouble / 60 / 60)
  //    println(costForAstrophysics())
  //  }

  test("Test strategies base") {
    val ogame = Ogame(
      resources = Resources(2_000, 700, 560),
      buildings = Map(MetalMine -> 8, CrystalMine -> 8, DeuteriumSynthesizer -> 5, SolarPlant -> 10)
    ).buildE(CrystalMine)
      .advanceTime(remainingCost())
    println(ogame)
    println(ogame.timeInSeconds.toDouble / 60 / 60)
    println(remainingCost())
  }

  test("Test strategies") {
    val ogame = Ogame(
      resources = Resources(2_000, 700, 560),
      buildings = Map(MetalMine -> 8, CrystalMine -> 8, DeuteriumSynthesizer -> 5, SolarPlant -> 10)
    ).buildE(CrystalMine)
      .advanceTime(remainingCost())
    println(ogame)
    println(ogame.timeInSeconds.toDouble / 60 / 60)
    println(remainingCost())
  }

  case class Ogame(
      productionFactor: Int = 10,
      timeInSeconds: Int = 0,
      resources: Resources = Resources(500, 500, 0),
      buildings: Map[SuppliesBuilding, Int] = Map(
        MetalMine -> 0,
        CrystalMine -> 0,
        DeuteriumSynthesizer -> 0,
        SolarPlant -> 0
      )
  ) {
    def advanceTime(collect: Resources): Ogame = {
      val hourlyProduction = currentProduction()
      val timeToMetal = (collect.metal - resources.metal).toDouble / hourlyProduction.metal
      val timeToCrystal = (collect.crystal - resources.crystal).toDouble / hourlyProduction.crystal
      val timeToDeuterium = (collect.deuterium - resources.deuterium).toDouble / hourlyProduction.deuterium
      val maxTime = Math.max(Math.max(timeToMetal, timeToCrystal), timeToDeuterium)
      advanceTime(Math.max(0, maxTime * 3600).toInt)
    }

    def advanceTime(seconds: Int): Ogame = {
      val hourlyProduction = currentProduction()
      val produced = Resources(
        metal = hourlyProduction.metal * seconds / 3600,
        crystal = hourlyProduction.crystal * seconds / 3600,
        deuterium = hourlyProduction.deuterium * seconds / 3600
      )
      this.copy(timeInSeconds = timeInSeconds + seconds, resources = resources.add(produced))
    }

    def currentProduction(): Resources = {
      val percentage = minesProductionFactor()
      val metalMineProduction = percentage * Production.metalMineHourlyProduction(buildings(MetalMine))
      val crystalMineProduction = percentage * Production.crystalMineHourlyProduction(buildings(CrystalMine))
      val deuteriumSynthesizerProduction = percentage * Production.deuteriumSynthesizerHourlyProduction(buildings(DeuteriumSynthesizer))
      Resources(
        metal = (productionFactor * (30 + metalMineProduction)).toInt,
        crystal = (productionFactor * (15 + crystalMineProduction)).toInt,
        deuterium = (productionFactor * deuteriumSynthesizerProduction).toInt
      )
    }

    def minesProductionFactor(): Double = {
      val energyConsumption = EnergyConsumption.metalEnergyConsumption(buildings(MetalMine)) +
        EnergyConsumption.crystalEnergyConsumption(buildings(CrystalMine)) +
        EnergyConsumption.deuteriumEnergyConsumption(buildings(DeuteriumSynthesizer))
      val energyProduction = EnergyConsumption.solarPlantEnergyProduction(buildings(SolarPlant))
      if (energyConsumption == 0) {
        0.0
      } else {
        Math.max(0, Math.min(1.0, energyProduction.toDouble / energyConsumption))
      }
    }

    def buildE(building: SuppliesBuilding): Ogame = {
      if (minesProductionFactor() < 1) {
        build(SolarPlant).build(building)
      } else {
        build(building)
      }
    }

    def build(building: SuppliesBuilding): Ogame = {
      val cost = SuppliesBuildingCosts.buildingCost(building, buildings(building) + 1)
      val withResources = advanceTime(cost)
      val newResources = Resources(
        resources.metal - cost.metal,
        resources.crystal - cost.crystal,
        resources.deuterium - cost.deuterium
      )
      withResources.copy(
        resources = newResources,
        buildings = buildings.map(entry => if (entry._1 == building) entry._1 -> (entry._2 + 1) else entry._1 -> entry._2)
      )
    }
  }

  private def remainingCost(): Resources = {
    List(
      TechnologyCosts.technologyCost(Espionage, 4),
      TechnologyCosts.technologyCost(ImpulseDrive, 1),
      //    TechnologyCosts.technologyCost(Shipyard, 2),
      FacilityBuildingCosts.buildingCost(Shipyard, 3),
      TechnologyCosts.technologyCost(ImpulseDrive, 2),
      FacilityBuildingCosts.buildingCost(Shipyard, 4),
      TechnologyCosts.technologyCost(ImpulseDrive, 3),
      TechnologyCosts.technologyCost(Computer, 1),
      TechnologyCosts.technologyCost(Computer, 2),
      TechnologyCosts.technologyCost(Astrophysics, 1)
    ).fold(Resources.Zero) { (one, other) =>
      one.add(other)
    }
  }

  private def costForAstrophysics(): Resources = {
    List(
      FacilityBuildingCosts.buildingCost(RoboticsFactory, 1),
      FacilityBuildingCosts.buildingCost(RoboticsFactory, 2),
      FacilityBuildingCosts.buildingCost(Shipyard, 1),
      FacilityBuildingCosts.buildingCost(Shipyard, 2),
      FacilityBuildingCosts.buildingCost(Shipyard, 3),
      FacilityBuildingCosts.buildingCost(Shipyard, 4),
      FacilityBuildingCosts.buildingCost(ResearchLab, 1),
      FacilityBuildingCosts.buildingCost(ResearchLab, 2),
      FacilityBuildingCosts.buildingCost(ResearchLab, 3),
      TechnologyCosts.technologyCost(Energy, 1),
      TechnologyCosts.technologyCost(CombustionDrive, 1),
      TechnologyCosts.technologyCost(CombustionDrive, 2),
      TechnologyCosts.technologyCost(ImpulseDrive, 1),
      TechnologyCosts.technologyCost(ImpulseDrive, 2),
      TechnologyCosts.technologyCost(ImpulseDrive, 3),
      TechnologyCosts.technologyCost(Espionage, 1),
      TechnologyCosts.technologyCost(Espionage, 2),
      TechnologyCosts.technologyCost(Espionage, 3),
      TechnologyCosts.technologyCost(Espionage, 4),
      TechnologyCosts.technologyCost(Astrophysics, 1),
      Resources(2_000, 0, 0),
      ShipCosts.shipCost(SmallCargoShip),
      ShipCosts.shipCost(ColonyShip)
    ).fold(Resources.Zero) { (one, other) =>
      one.add(other)
    }
  }
}
