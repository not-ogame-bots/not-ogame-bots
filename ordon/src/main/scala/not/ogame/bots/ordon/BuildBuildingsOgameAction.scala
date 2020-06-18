package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FacilityBuilding.{ResearchLab, RoboticsFactory, Shipyard}
import not.ogame.bots.SuppliesBuilding.{CrystalMine, DeuteriumSynthesizer, MetalMine, SolarPlant}
import not.ogame.bots._
import not.ogame.bots.facts.{FacilityBuildingCosts, SuppliesBuildingCosts}

class BuildBuildingsOgameAction[T[_]: Monad](planet: PlayerPlanet)(implicit clock: LocalClock) extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      page <- ogame.readSuppliesPage(planet.id)
      resumeOn <- if (page.currentBuildingProgress.isDefined) {
        page.currentBuildingProgress.get.finishTimestamp.pure[T]
      } else {
        buildOrWaitForResources(ogame, page)
      }
    } yield resumeOn

  def isAnyPositive(resources: Resources): Boolean = {
    resources.metal > 0 || resources.crystal > 0 || resources.deuterium > 0
  }

  private def buildOrWaitForResources(ogame: OgameDriver[T], page: SuppliesPageData): T[ZonedDateTime] = {
    val maybeTask = getNextBuilding(page)
    if (maybeTask.isDefined) {
      val task: TaskOnPlanet = maybeTask.get
      val cost = task.cost()
      val currentResources = page.currentResources
      val deficit = minus(cost, currentResources)
      if (isAnyPositive(deficit)) {
        var maxTimeInHours = 0.0
        if (deficit.metal > 0) {
          if (page.currentProduction.metal > 0) {
            maxTimeInHours = Math.max(maxTimeInHours, deficit.metal.toDouble / page.currentProduction.metal)
          }
        }
        if (deficit.crystal > 0) {
          if (page.currentProduction.crystal > 0) {
            maxTimeInHours = Math.max(maxTimeInHours, deficit.crystal.toDouble / page.currentProduction.crystal)
          }
        }
        if (deficit.deuterium > 0) {
          if (page.currentProduction.deuterium > 0) {
            maxTimeInHours = Math.max(maxTimeInHours, deficit.deuterium.toDouble / page.currentProduction.deuterium)
          }
        }
        val maxTimeInSeconds = maxTimeInHours * 3600
        clock.now().plusSeconds(maxTimeInSeconds.toInt).pure[T]
      } else {
        task.construct(ogame, planet)
      }
    } else {
      clock.now().plusDays(1).pure[T]
    }
  }

  private def minus(one: Resources, other: Resources): Resources = {
    Resources(one.metal - other.metal, one.crystal - other.crystal, one.deuterium - other.deuterium)
  }

  private def getNextBuilding(page: SuppliesPageData): Option[TaskOnPlanet] = {
    if (page.currentResources.energy < 0) {
      Option.apply(new SuppliesBuildingTask(SolarPlant, page.getLevel(SolarPlant).value + 1))
    } else {
      taskQueue().find(p => p.isValid(page))
    }
  }

  private def taskQueue(): List[TaskOnPlanet] = {
    List(
      new SuppliesBuildingTask(MetalMine, 1),
      new SuppliesBuildingTask(MetalMine, 2),
      new SuppliesBuildingTask(MetalMine, 3),
      new SuppliesBuildingTask(MetalMine, 4),
      new SuppliesBuildingTask(MetalMine, 5),
      new SuppliesBuildingTask(CrystalMine, 1),
      new SuppliesBuildingTask(CrystalMine, 2),
      new SuppliesBuildingTask(CrystalMine, 3),
      new SuppliesBuildingTask(MetalMine, 6),
      new SuppliesBuildingTask(CrystalMine, 4),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 1),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 2),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 3),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 4),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 5),
      new SuppliesBuildingTask(CrystalMine, 5),
      new SuppliesBuildingTask(CrystalMine, 6),
      new FacilityBuildingTask(RoboticsFactory, 1),
      new FacilityBuildingTask(RoboticsFactory, 2),
      new FacilityBuildingTask(Shipyard, 1),
      new FacilityBuildingTask(ResearchLab, 1)
    )
  }
}

class SuppliesBuildingTask(suppliesBuilding: SuppliesBuilding, level: Int)(implicit clock: LocalClock) extends TaskOnPlanet {
  override def isValid(page: SuppliesPageData): Boolean = page.getLevel(suppliesBuilding).value < level

  override def cost(): Resources = SuppliesBuildingCosts.buildingCost(suppliesBuilding, level)

  override def construct[T[_]: Monad](ogameDriver: OgameDriver[T], planet: PlayerPlanet): T[ZonedDateTime] =
    for {
      _ <- ogameDriver.buildSuppliesBuilding(planet.id, suppliesBuilding)
      page <- ogameDriver.readSuppliesPage(planet.id)
      resumeOn = page.currentBuildingProgress.map(_.finishTimestamp).getOrElse(clock.now())
    } yield resumeOn
}

class FacilityBuildingTask(facilityBuilding: FacilityBuilding, level: Int)(implicit clock: LocalClock) extends TaskOnPlanet {
  override def isValid(page: SuppliesPageData): Boolean = false

  override def cost(): Resources = FacilityBuildingCosts.buildingCost(facilityBuilding, level)

  override def construct[T[_]: Monad](ogameDriver: OgameDriver[T], planet: PlayerPlanet): T[ZonedDateTime] =
    for {
      _ <- ogameDriver.buildFacilityBuilding(planet.id, facilityBuilding)
      page <- ogameDriver.readSuppliesPage(planet.id)
      resumeOn = page.currentBuildingProgress.map(_.finishTimestamp).getOrElse(clock.now())
    } yield resumeOn
}

//class TechnologyBuildingTask(technology: Technology, level: Int)(implicit clock: LocalClock) extends TaskOnPlanet {
//  override def cost(): Resources = TechnologyCosts.technologyCost(technology, level)
//
//  override def construct[T[_]: Monad](ogameDriver: OgameDriver[T], planet: PlayerPlanet): T[ZonedDateTime] =
//    for {
//      _ <- ogameDriver.buildTechnology(planet.id, technology)
//      page <- ogameDriver.readTechnologyPage(planet.id)
//      resumeOn = page.currentResearchProgress.map(_.finishTimestamp).getOrElse(clock.now())
//    } yield resumeOn
//}

trait TaskOnPlanet {
  def isValid(page: SuppliesPageData): Boolean

  def cost(): Resources

  def construct[T[_]: Monad](ogameDriver: OgameDriver[T], planet: PlayerPlanet): T[ZonedDateTime]
}
