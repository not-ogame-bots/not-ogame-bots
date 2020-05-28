package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import cats.implicits._
import eu.timepit.refined.numeric.Positive
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots.facts.{FacilityBuildingCosts, SuppliesBuildingCosts}
import not.ogame.bots.ghostbuster.{BotConfig, FLogger, Wish}
import not.ogame.bots.selenium.refineVUnsafe
import not.ogame.bots.{FacilityBuilding, FacilityPageData, PlayerPlanet, Resources, SuppliesBuilding, SuppliesPageData}

class Builder(taskExecutor: TaskExecutor, botConfig: BotConfig) extends FLogger {
  def buildNextThingFromWishList(planet: PlayerPlanet): Task[Option[ZonedDateTime]] = {
    for {
      sp <- taskExecutor.readSupplyPage(planet)
      fp <- taskExecutor.readFacilityPage(planet)
      modifiedSp = sp.copy(currentResources = fp.currentResources)
      time <- buildNextThingFromWishList(planet, modifiedSp, fp)
    } yield time
  }

  private def buildNextThingFromWishList(planet: PlayerPlanet, suppliesPageData: SuppliesPageData, facilityPageData: FacilityPageData) = {
    if (!suppliesPageData.buildingInProgress) {
      botConfig.wishlist
        .collectFirst {
          case w: Wish.BuildSupply if suppliesPageData.getLevel(w.suppliesBuilding).value < w.level.value && w.planetId == planet.id =>
            buildSupplyBuildingOrNothing(w.suppliesBuilding, suppliesPageData, planet)
          case w: Wish.BuildFacility if facilityPageData.getLevel(w.facilityBuilding).value < w.level.value && w.planetId == planet.id =>
            buildFacilityBuildingOrNothing(w.facilityBuilding, facilityPageData, suppliesPageData, planet)
          case w: Wish.SmartSupplyBuilder if isSmartBuilderApplicable(planet, suppliesPageData, w) =>
            smartBuilder(planet, suppliesPageData, w)
        }
        .sequence
        .map(_.flatten)
    } else {
      suppliesPageData.currentBuildingProgress.map(_.finishTimestamp).pure[Task]
    }
  }

  private def buildSupplyBuildingOrNothing(suppliesBuilding: SuppliesBuilding, suppliesPageData: SuppliesPageData, planet: PlayerPlanet) = {
    val level = nextLevel(suppliesPageData, suppliesBuilding)
    val requiredResources =
      SuppliesBuildingCosts.buildingCost(suppliesBuilding, level)
    if (suppliesPageData.currentResources.gtEqTo(requiredResources)) {
      taskExecutor.buildSupplyBuilding(suppliesBuilding, level, planet).map(Some(_))
    } else {
      Logger[Task]
        .info(
          s"Wanted to build $suppliesBuilding $level but there were not enough resources on ${planet.coordinates} " +
            s"- ${suppliesPageData.currentResources}/$requiredResources"
        )
        .map(_ => None)
    }
  }

  private def buildFacilityBuildingOrNothing(
      facilityBuilding: FacilityBuilding,
      facilityPageData: FacilityPageData,
      suppliesPageData: SuppliesPageData,
      planet: PlayerPlanet
  ) = {
    val level = nextLevel(facilityPageData, facilityBuilding)
    val requiredResources = FacilityBuildingCosts.buildingCost(facilityBuilding, level)
    if (!suppliesPageData.shipInProgress) {
      if (facilityPageData.currentResources.gtEqTo(requiredResources)) {
        taskExecutor.buildFacilityBuilding(facilityBuilding, level, planet).map(Some(_))
      } else {
        Logger[Task]
          .info(
            s"Wanted to build $facilityBuilding $level but there were not enough resources on ${planet.coordinates}" +
              s"- ${suppliesPageData.currentResources}/$requiredResources"
          )
          .map(_ => None)
      }
    } else {
      Logger[Task]
        .info(s"Wanted to build $facilityBuilding but there were some ships building") >>
        suppliesPageData.currentShipyardProgress.map(_.finishTimestamp).pure[Task]
    }
  }

  private def nextLevel(suppliesPage: SuppliesPageData, building: SuppliesBuilding) = {
    refineVUnsafe[Positive, Int](suppliesPage.suppliesLevels.values(building).value + 1)
  }

  private def nextLevel(facilityPageData: FacilityPageData, building: FacilityBuilding) = {
    refineVUnsafe[Positive, Int](facilityPageData.facilityLevels.values(building).value + 1)
  }

  private def smartBuilder(planet: PlayerPlanet, suppliesPageData: SuppliesPageData, w: Wish.SmartSupplyBuilder) = {
    if (suppliesPageData.currentResources.energy < 0) {
      buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.SolarPlant)
    } else { //TODO can we get rid of hardcoded ratio?
      val shouldBuildDeuter = suppliesPageData.getLevel(SuppliesBuilding.MetalMine).value -
        suppliesPageData.getLevel(SuppliesBuilding.DeuteriumSynthesizer).value > 2 &&
        suppliesPageData.getLevel(SuppliesBuilding.DeuteriumSynthesizer).value < w.deuterLevel.value
      val shouldBuildCrystal = suppliesPageData.getLevel(SuppliesBuilding.MetalMine).value -
        suppliesPageData.getLevel(SuppliesBuilding.CrystalMine).value > 2 &&
        suppliesPageData.getLevel(SuppliesBuilding.CrystalMine).value < w.crystalLevel.value
      if (shouldBuildDeuter) {
        buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.DeuteriumSynthesizer)
      } else if (shouldBuildCrystal) {
        buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.CrystalMine)
      } else if (suppliesPageData.getLevel(SuppliesBuilding.MetalMine).value < w.metalLevel.value) {
        buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.MetalMine)
      } else {
        Option.empty[ZonedDateTime].pure[Task]
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
  ): Task[Option[ZonedDateTime]] = {
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
    val metalMineUnderLevel = w.metalLevel.value > suppliesPageData.getLevel(SuppliesBuilding.MetalMine).value
    val crystalMineUnderLevel = w.crystalLevel.value > suppliesPageData.getLevel(SuppliesBuilding.CrystalMine).value
    val deuterMineUnderLevel = w.deuterLevel.value > suppliesPageData.getLevel(SuppliesBuilding.DeuteriumSynthesizer).value
    correctPlanet && (metalMineUnderLevel || crystalMineUnderLevel || deuterMineUnderLevel)
  }
}
