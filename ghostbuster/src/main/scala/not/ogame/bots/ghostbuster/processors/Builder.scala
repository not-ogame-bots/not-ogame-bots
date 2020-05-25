package not.ogame.bots.ghostbuster.processors

import java.time.Instant

import eu.timepit.refined.numeric.Positive
import monix.eval.Task
import not.ogame.bots.facts.SuppliesBuildingCosts
import not.ogame.bots.{PlayerPlanet, Resources, SuppliesBuilding, SuppliesPageData}
import not.ogame.bots.ghostbuster.{BotConfig, FLogger, Wish}
import not.ogame.bots.selenium.refineVUnsafe
import cats.implicits._
import io.chrisdavenport.log4cats.Logger

class Builder(taskExecutor: TaskExecutor, botConfig: BotConfig) extends FLogger {
  def buildNextThingFromWishList(planet: PlayerPlanet): Task[Option[Instant]] = {
    taskExecutor.readSupplyPage(planet).flatMap { suppliesPageData =>
      if (!suppliesPageData.buildingInProgress) {
        botConfig.wishlist
          .collectFirst {
            case w: Wish.BuildSupply if suppliesPageData.getLevel(w.suppliesBuilding) < w.level.value && w.planetId == planet.id =>
              if (!suppliesPageData.buildingInProgress) {
                buildSupplyBuildingOrNothing(w.suppliesBuilding, suppliesPageData, planet)
              } else {
                suppliesPageData.currentBuildingProgress.map(_.finishTimestamp).pure[Task]
              }
            case w: Wish.SmartSupplyBuilder if isSmartBuilderApplicable(planet, suppliesPageData, w) =>
              if (!suppliesPageData.buildingInProgress) {
                smartBuilder(planet, suppliesPageData, w)
              } else {
                suppliesPageData.currentBuildingProgress.map(_.finishTimestamp).pure[Task]
              }
          }
          .sequence
          .map(_.flatten)
      } else {
        suppliesPageData.currentBuildingProgress.map(_.finishTimestamp).pure[Task]
      }
    }
  }

  private def buildSupplyBuildingOrNothing(suppliesBuilding: SuppliesBuilding, suppliesPageData: SuppliesPageData, planet: PlayerPlanet) = {
    val level = nextLevel(suppliesPageData, suppliesBuilding)
    val requiredResources =
      SuppliesBuildingCosts.buildingCost(suppliesBuilding, level)
    if (suppliesPageData.currentResources.gtEqTo(requiredResources)) {
      taskExecutor.buildSupplyBuilding(suppliesBuilding, level, planet).map(Some(_))
    } else {
      Logger[Task].info(s"Wanted to build $suppliesBuilding but there were not enough resources").map(_ => None)
    }
  }

  private def nextLevel(suppliesPage: SuppliesPageData, building: SuppliesBuilding) = {
    refineVUnsafe[Positive, Int](suppliesPage.suppliesLevels.values(building).value + 1)
  }

  private def smartBuilder(planet: PlayerPlanet, suppliesPageData: SuppliesPageData, w: Wish.SmartSupplyBuilder) = {
    if (suppliesPageData.currentResources.energy < 0) {
      buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.SolarPlant)
    } else {
      val shouldBuildDeuter = suppliesPageData.getLevel(SuppliesBuilding.MetalMine) -
        suppliesPageData.getLevel(SuppliesBuilding.DeuteriumSynthesizer) > 2 &&
        suppliesPageData.getLevel(SuppliesBuilding.DeuteriumSynthesizer) < w.deuterLevel.value
      val shouldBuildCrystal = suppliesPageData.getLevel(SuppliesBuilding.MetalMine) -
        suppliesPageData.getLevel(SuppliesBuilding.CrystalMine) > 2 &&
        suppliesPageData.getLevel(SuppliesBuilding.CrystalMine) < w.crystalLevel.value
      if (shouldBuildDeuter) {
        buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.DeuteriumSynthesizer)
      } else if (shouldBuildCrystal) {
        buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.CrystalMine)
      } else if (suppliesPageData.getLevel(SuppliesBuilding.MetalMine) < w.metalLevel.value) {
        buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.MetalMine)
      } else {
        Option.empty[Instant].pure[Task]
      }
    }
  }

  private def buildBuildingOrStorage(planet: PlayerPlanet, suppliesPageData: SuppliesPageData, building: SuppliesBuilding) = {
    val level = nextLevel(suppliesPageData, building)
    val requiredResources = SuppliesBuildingCosts.buildingCost(building, level)
    if (suppliesPageData.currentCapacity.gtEqTo(requiredResources)) {
      buildSupplyBuildingOrNothing(building, suppliesPageData, planet)
    } else {
      buildStorage(suppliesPageData, requiredResources, planet)
    }
  }

  private def buildStorage(
      suppliesPage: SuppliesPageData,
      requiredResources: Resources,
      planet: PlayerPlanet
  ): Task[Option[Instant]] = {
    requiredResources.difference(suppliesPage.currentCapacity) match {
      case Resources(m, _, _, _) if m > 0 =>
        buildSupplyBuildingOrNothing(SuppliesBuilding.MetalStorage, suppliesPage, planet)
      case Resources(_, c, _, _) if c > 0 =>
        buildSupplyBuildingOrNothing(SuppliesBuilding.CrystalStorage, suppliesPage, planet)
      case Resources(_, _, d, _) if d > 0 =>
        buildSupplyBuildingOrNothing(SuppliesBuilding.DeuteriumStorage, suppliesPage, planet)
    }
  }

  private def isSmartBuilderApplicable(planet: PlayerPlanet, suppliesPageData: SuppliesPageData, w: Wish.SmartSupplyBuilder) = {
    val correctPlanet = w.planetId == planet.id
    val metalMineUnderLevel = w.metalLevel.value > suppliesPageData.getLevel(SuppliesBuilding.MetalMine)
    val crystalMineUnderLevel = w.crystalLevel.value > suppliesPageData.getLevel(SuppliesBuilding.CrystalMine)
    val deuterMineUnderLevel = w.deuterLevel.value > suppliesPageData.getLevel(SuppliesBuilding.DeuteriumSynthesizer)
    correctPlanet && (metalMineUnderLevel || crystalMineUnderLevel || deuterMineUnderLevel)
  }
}
