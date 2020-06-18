package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.SuppliesBuilding.{CrystalMine, DeuteriumSynthesizer, MetalMine, SolarPlant}
import not.ogame.bots._
import not.ogame.bots.facts.SuppliesBuildingCosts

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
    val maybeBuilding = getNextBuilding(page)
    if (maybeBuilding.isDefined) {
      val toBuild = maybeBuilding.get
      val nextLevel = page.getLevel(toBuild).value + 1
      val cost = SuppliesBuildingCosts.buildingCost(toBuild, nextLevel)
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
        ogame.buildSuppliesBuilding(planet.id, toBuild).map(_ => clock.now())
      }
    } else {
      clock.now().plusDays(1).pure[T]
    }
  }

  private def minus(one: Resources, other: Resources): Resources = {
    Resources(one.metal - other.metal, one.crystal - other.crystal, one.deuterium - other.deuterium)
  }

  private def getNextBuilding(page: SuppliesPageData): Option[SuppliesBuilding] = {
    if (page.currentResources.energy < 0) {
      Option.apply(SolarPlant)
    } else {
      buildingQueue().find(p => page.getLevel(p._1).value < p._2).map(_._1)
    }
  }

  private def buildingQueue(): List[(SuppliesBuilding, Int)] = {
    List(
      MetalMine -> 1,
      MetalMine -> 2,
      MetalMine -> 3,
      MetalMine -> 4,
      MetalMine -> 5,
      CrystalMine -> 1,
      CrystalMine -> 2,
      CrystalMine -> 3,
      MetalMine -> 6,
      CrystalMine -> 4,
      DeuteriumSynthesizer -> 1,
      DeuteriumSynthesizer -> 2,
      DeuteriumSynthesizer -> 3,
      DeuteriumSynthesizer -> 4,
      DeuteriumSynthesizer -> 5,
      CrystalMine -> 5,
      CrystalMine -> 6
    )
  }
}
