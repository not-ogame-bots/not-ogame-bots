package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import not.ogame.bots._
import not.ogame.bots.facts.{FacilityBuildingCosts, ShipCosts, SuppliesBuildingCosts, TechnologyCosts}
import not.ogame.bots.ghostbuster.executor._
import not.ogame.bots.ghostbuster.ogame.OgameAction
import not.ogame.bots.ghostbuster.FLogger

import scala.math.BigDecimal.RoundingMode

class Builder(ogameActionDriver: OgameDriver[OgameAction], wishlist: List[Wish])(
    implicit clock: LocalClock
) extends FLogger {
  def buildNextThingFromWishList(planet: PlayerPlanet): OgameAction[BuilderResult] = {
    val wishesForPlanet = wishlist.filter(_.planetId == planet.id)
    if (wishesForPlanet.nonEmpty) {
      for {
        sp <- ogameActionDriver.readSuppliesPage(planet.id)
        fp <- ogameActionDriver.readFacilityPage(planet.id)
        tp <- ogameActionDriver.readTechnologyPage(planet.id)
        mfp <- ogameActionDriver.readFleetPage(planet.id)
        time <- buildNextThingFromWishList(planet, sp, fp, tp, mfp.ships, wishesForPlanet)
      } yield time
    } else {
      BuilderResult.idle().pure[OgameAction]
    }
  }

  private def buildNextThingFromWishList(
      planet: PlayerPlanet,
      suppliesPageData: SuppliesPageData,
      facilityPageData: FacilityPageData,
      technologyPageData: TechnologyPageData,
      fleet: Map[ShipType, Int],
      wishesForPlanet: List[Wish]
  ): OgameAction[BuilderResult] = {
    wishesForPlanet
      .collectFirst {
        case w: Wish.BuildSupply if suppliesPageData.getIntLevel(w.suppliesBuilding) < w.level =>
          buildSupplyBuildingOrNothing(w.suppliesBuilding, suppliesPageData, planet)
        case w: Wish.BuildFacility if facilityPageData.getIntLevel(w.facilityBuilding) < w.level =>
          buildFacilityBuildingOrNothing(w.facilityBuilding, facilityPageData, suppliesPageData, planet)
        case w: Wish.SmartSupplyBuilder if isSmartBuilderApplicable(planet, suppliesPageData, w) =>
          smartBuilder(planet, suppliesPageData, w)
        case w: Wish.BuildShip if fleet(w.shipType) <= w.amount =>
          buildShips(planet, w, suppliesPageData)
        case w: Wish.Research if technologyPageData.getIntLevel(w.technology) < w.level =>
          startResearch(planet, w.technology, technologyPageData)
        case w: Wish.DeuterBuilder if suppliesPageData.getIntLevel(SuppliesBuilding.DeuteriumSynthesizer) < w.level =>
          onlyDeuterBuilder(planet, suppliesPageData)
      }
      .sequence
      .map(_.getOrElse(BuilderResult.idle()))
  }

  private def startResearch(planet: PlayerPlanet, technology: Technology, technologyPageData: TechnologyPageData) = {
    technologyPageData.currentResearchProgress match {
      case Some(value) =>
        Logger[OgameAction]
          .info(s"${showCoordinates(planet)} Wanted to build $technology but there were some other research ongoing")
          .map(_ => BuilderResult.building(value.finishTimestamp))
      case None =>
        val level = technologyPageData.getIntLevel(technology) + 1
        val requiredResources = TechnologyCosts.technologyCost(technology, level)
        if (technologyPageData.currentResources.gtEqTo(requiredResources)) {
          ogameActionDriver.startResearch(planet.id, technology) >> ogameActionDriver
            .readTechnologyPage(planet.id)
            .map(t => BuilderResult.building(t.currentResearchProgress.get.finishTimestamp))
        } else {
          val secondsToWait = calculateWaitingTime(
            requiredResources = requiredResources,
            production = technologyPageData.currentProduction,
            resources = technologyPageData.currentResources
          )
          Logger[OgameAction]
            .info(
              s"${showCoordinates(planet)} Wanted to build $technology $level but there were not enough resources on ${planet.coordinates} " +
                s"- ${technologyPageData.currentResources}/$requiredResources"
            )
            .map(_ => BuilderResult.waiting(clock.now().plusSeconds(secondsToWait)))
        }
    }
  }

  private def buildShips(planet: PlayerPlanet, w: Wish.BuildShip, suppliesPageData: SuppliesPageData) = { //TODO check not building and shipyard is not upgrading
    val requiredResourcesSingleShip = ShipCosts.shipCost(w.shipType)
    if (suppliesPageData.currentResources.gtEqTo(requiredResourcesSingleShip)) {
      val canBuildAmount = suppliesPageData.currentResources.div(requiredResourcesSingleShip).min
      val buildAmount = Math.min(canBuildAmount, w.amount).toInt
      ogameActionDriver
        .buildShipAndGetTime(planet.id, w.shipType, buildAmount)
        .map(s => BuilderResult.building(s.finishTimestamp))
    } else {
      val secondsToWait =
        calculateWaitingTime(requiredResourcesSingleShip, suppliesPageData.currentProduction, suppliesPageData.currentResources)
      Logger[OgameAction]
        .info(
          s"${showCoordinates(planet)} Wanted to build $w but there were not enough resources on" +
            s"- ${suppliesPageData.currentResources}/$requiredResourcesSingleShip"
        )
        .map(_ => BuilderResult.waiting(clock.now().plusSeconds(secondsToWait)))
    }
  }

  private def buildSupplyBuildingOrNothing(suppliesBuilding: SuppliesBuilding, suppliesPageData: SuppliesPageData, planet: PlayerPlanet) = {
    suppliesPageData.currentBuildingProgress match {
      case Some(value) =>
        Logger[OgameAction]
          .info(s"${showCoordinates(planet)} Wanted to build $suppliesBuilding but something was being built")
          .map(_ => BuilderResult.building(value.finishTimestamp))
      case None =>
        val level = suppliesPageData.getIntLevel(suppliesBuilding) + 1
        val requiredResources = SuppliesBuildingCosts.buildingCost(suppliesBuilding, level)
        if (suppliesPageData.currentResources.gtEqTo(requiredResources)) {
          Logger[OgameAction].info(s"${showCoordinates(planet)} Building $suppliesBuilding $level") >>
            ogameActionDriver
              .buildSupplyAndGetTime(planet.id, suppliesBuilding)
              .map(s => BuilderResult.building(s.finishTimestamp))
        } else {
          val secondsToWait = calculateWaitingTime(requiredResources, suppliesPageData.currentProduction, suppliesPageData.currentResources)
          Logger[OgameAction]
            .info(
              s"${showCoordinates(planet)} Wanted to build $suppliesBuilding $level but there were not enough resources on ${planet.coordinates} " +
                s"- ${suppliesPageData.currentResources}/$requiredResources"
            )
            .map { _ =>
              BuilderResult.waiting(clock.now().plusSeconds(secondsToWait))
            }
        }
    }
  }

  private def calculateWaitingTime(requiredResources: Resources, production: Resources, resources: Resources) = {
    val missingResources = requiredResources.difference(resources)
    val hoursToWait = missingResources.div(production).max
    (hoursToWait * 3600).toInt
  }

  private def buildFacilityBuildingOrNothing(
      facilityBuilding: FacilityBuilding,
      facilityPageData: FacilityPageData,
      suppliesPageData: SuppliesPageData,
      planet: PlayerPlanet
  ) = {
    suppliesPageData.currentShipyardProgress.orElse(suppliesPageData.currentBuildingProgress) match {
      case Some(value) =>
        Logger[OgameAction]
          .info(s"${showCoordinates(planet)} Wanted to build $facilityBuilding but there were some ships building")
          .as(BuilderResult.building(value.finishTimestamp))
      case None =>
        val level = facilityPageData.getIntLevel(facilityBuilding) + 1
        val requiredResources = FacilityBuildingCosts.buildingCost(facilityBuilding, level)
        if (facilityPageData.currentResources.gtEqTo(requiredResources)) {
          Logger[OgameAction].info(s"${showCoordinates(planet)} Building $facilityBuilding $level") >>
            ogameActionDriver
              .buildFacilityAndGetTime(planet.id, facilityBuilding)
              .map(f => BuilderResult.building(f.finishTimestamp))
        } else {
          val secondsToWait = calculateWaitingTime(requiredResources, suppliesPageData.currentProduction, suppliesPageData.currentResources)
          Logger[OgameAction]
            .info(
              s"${showCoordinates(planet)} Wanted to build $facilityBuilding $level but there were not enough resources" +
                s"- ${suppliesPageData.currentResources}/$requiredResources"
            )
            .as(BuilderResult.waiting(clock.now().plusSeconds(secondsToWait)))
        }
    }
  }

  private def smartBuilder(planet: PlayerPlanet, suppliesPageData: SuppliesPageData, w: Wish.SmartSupplyBuilder) = {
    suppliesPageData.currentBuildingProgress match {
      case Some(value) =>
        Logger[OgameAction]
          .info(s"${showCoordinates(planet)} Wanted to run smart builder but sth was being built")
          .map(_ => BuilderResult.building(value.finishTimestamp))
      case None =>
        if (suppliesPageData.currentResources.energy < 0) {
          if (suppliesPageData.getIntLevel(SuppliesBuilding.SolarPlant) >= 20) {
            val amount = (BigDecimal(Math.abs(suppliesPageData.currentResources.energy)) / 30).setScale(0, RoundingMode.UP).toInt
            buildSolarSatellite(planet, suppliesPageData, amount)
          } else {
            buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.SolarPlant)
          }
        } else { //TODO can we get rid of hardcoded ratio?
          val deuterLevel = suppliesPageData.getIntLevel(SuppliesBuilding.DeuteriumSynthesizer)
          val crystalLevel = suppliesPageData.getIntLevel(SuppliesBuilding.CrystalMine)
          val metalLevel = suppliesPageData.getIntLevel(SuppliesBuilding.MetalMine)
          if (metalLevel < 20) {
            val shouldBuildDeuter = metalLevel - deuterLevel > 4 && deuterLevel < w.deuterLevel
            val shouldBuildCrystal = metalLevel - crystalLevel > 2 && crystalLevel < w.crystalLevel
            if (shouldBuildCrystal) {
              buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.CrystalMine)
            } else if (shouldBuildDeuter) {
              buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.DeuteriumSynthesizer)
            } else if (metalLevel < w.metalLevel) {
              buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.MetalMine)
            } else {
              BuilderResult.idle().pure[OgameAction]
            }
          } else {
            val shouldBuildDeuter = crystalLevel - deuterLevel >= 1 && deuterLevel < w.deuterLevel
            val shouldBuildCrystal = crystalLevel < w.crystalLevel
            if (shouldBuildDeuter) {
              buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.DeuteriumSynthesizer)
            } else if (shouldBuildCrystal) {
              buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.CrystalMine)
            } else {
              BuilderResult.idle().pure[OgameAction]
            }
          }
        }
    }
  }

  private def onlyDeuterBuilder(planet: PlayerPlanet, suppliesPageData: SuppliesPageData) = {
    (suppliesPageData.currentBuildingProgress match {
      case Some(value) =>
        Logger[OgameAction]
          .info(s"${showCoordinates(planet)} Wanted to run deuter builder but sth was being built")
          .map(_ => BuilderResult.building(value.finishTimestamp))
      case None =>
        if (suppliesPageData.currentResources.energy < 0) {
          if (suppliesPageData.getIntLevel(SuppliesBuilding.SolarPlant) >= 18) {
            buildSupplyBuildingOrNothing(SuppliesBuilding.FusionPlant, suppliesPageData, planet)
          } else {
            buildSupplyBuildingOrNothing(SuppliesBuilding.SolarPlant, suppliesPageData, planet)
          }
        } else {
          if (suppliesPageData.currentCapacity.deuterium - suppliesPageData.currentResources.deuterium < 1000) {
            buildSupplyBuildingOrNothing(SuppliesBuilding.DeuteriumStorage, suppliesPageData, planet)
          } else {
            buildSupplyBuildingOrNothing(SuppliesBuilding.DeuteriumSynthesizer, suppliesPageData, planet)
          }
        }
    }).map {
      case BuilderResult.Building(finishTime) => BuilderResult.Building(finishTime)
      case BuilderResult.Waiting(_)           => BuilderResult.Idle
      case BuilderResult.Idle                 => BuilderResult.Idle
    }
  }

  private val SolarSatelliteCost = Resources(0, 2000, 500)

  private def buildSolarSatellite(planet: PlayerPlanet, suppliesPageData: SuppliesPageData, amount: Int) = {
    suppliesPageData.currentShipyardProgress match {
      case Some(value) => BuilderResult.building(value.finishTimestamp).pure[OgameAction]
      case None =>
        if (suppliesPageData.currentResources.gtEqTo(SolarSatelliteCost)) {
          Logger[OgameAction].info(s"${showCoordinates(planet)} Building solar satellite 1") >>
            ogameActionDriver.buildSolarSatellites(planet.id, amount) >>
            ogameActionDriver
              .readSuppliesPage(planet.id)
              .map(f => f.currentShipyardProgress.map(p => BuilderResult.building(p.finishTimestamp)).getOrElse(BuilderResult.idle()))
        } else {
          val secondsToWait =
            calculateWaitingTime(SolarSatelliteCost, suppliesPageData.currentProduction, suppliesPageData.currentResources)
          Logger[OgameAction]
            .info(
              s"${showCoordinates(planet)} Wanted to build solar satellite but there were not enough resources on" +
                s"- ${suppliesPageData.currentResources}/$SolarSatelliteCost"
            )
            .as(BuilderResult.building(clock.now().plusSeconds(secondsToWait)))
        }
    }
  }

  private def buildBuildingOrStorage(planet: PlayerPlanet, suppliesPageData: SuppliesPageData, building: SuppliesBuilding) = {
    val level = suppliesPageData.getIntLevel(building) + 1
    val requiredResources = SuppliesBuildingCosts.buildingCost(building, level)
    if (suppliesPageData.currentResources.gtEqTo(requiredResources) || suppliesPageData.currentCapacity.gtEqTo(requiredResources)) {
      buildSupplyBuildingOrNothing(building, suppliesPageData, planet)
    } else {
      buildStorage(suppliesPageData, requiredResources, planet)
    }
  }

  private def buildStorage(
      suppliesPage: SuppliesPageData,
      requiredResources: Resources,
      planet: PlayerPlanet
  ) = {
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
    val metalMineUnderLevel = w.metalLevel > suppliesPageData.getIntLevel(SuppliesBuilding.MetalMine)
    val crystalMineUnderLevel = w.crystalLevel > suppliesPageData.getIntLevel(SuppliesBuilding.CrystalMine)
    val deuterMineUnderLevel = w.deuterLevel > suppliesPageData.getIntLevel(SuppliesBuilding.DeuteriumSynthesizer)
    correctPlanet && (metalMineUnderLevel || crystalMineUnderLevel || deuterMineUnderLevel)
  }
}

sealed trait BuilderResult extends Product with Serializable
object BuilderResult {
  case class Building(finishTime: ZonedDateTime) extends BuilderResult
  case class Waiting(waitingTime: ZonedDateTime) extends BuilderResult
  case object Idle extends BuilderResult

  def building(finishTime: ZonedDateTime): BuilderResult = Building(finishTime)
  def waiting(waitingTime: ZonedDateTime): BuilderResult = Waiting(waitingTime)
  def idle(): BuilderResult = Idle
}

sealed trait Wish {
  def planetId: PlanetId
}
object Wish {
  case class BuildSupply(suppliesBuilding: SuppliesBuilding, level: Int, planetId: PlanetId) extends Wish

  case class BuildFacility(facilityBuilding: FacilityBuilding, level: Int, planetId: PlanetId) extends Wish

  case class BuildShip(shipType: ShipType, planetId: PlanetId, amount: Int) extends Wish

  case class SmartSupplyBuilder(
      metalLevel: Int,
      crystalLevel: Int,
      deuterLevel: Int,
      planetId: PlanetId
  ) extends Wish

  case class Research(technology: Technology, level: Int, planetId: PlanetId) extends Wish

  case class DeuterBuilder(level: Int, planetId: PlanetId) extends Wish
}
