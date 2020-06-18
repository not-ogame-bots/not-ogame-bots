package not.ogame.bots.facts

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV
import not.ogame.bots.Technology._
import not.ogame.bots.{Resources, Technology}

object TechnologyCosts {
  def technologyCost(technology: Technology, level: Int): Resources = {
    technologyCost(technology, refineVUnsafe[Positive, Int](level))
  }

  def technologyCost(technology: Technology, level: Int Refined Positive): Resources = {
    technology match {
      case Energy          => fromBaseCostPowerOf2(Resources(0, 800, 400), level)
      case Laser           => fromBaseCostPowerOf2(Resources(200, 100, 0), level)
      case Ion             => fromBaseCostPowerOf2(Resources(1_000, 300, 100), level)
      case Hyperspace      => fromBaseCostPowerOf2(Resources(0, 4_000, 2_000), level)
      case Plasma          => fromBaseCostPowerOf2(Resources(2_000, 4_000, 1_000), level)
      case CombustionDrive => fromBaseCostPowerOf2(Resources(400, 0, 600), level)
      case ImpulseDrive    => fromBaseCostPowerOf2(Resources(2_000, 4_000, 600), level)
      case HyperspaceDrive => fromBaseCostPowerOf2(Resources(10_000, 20_000, 6_000), level)
      case Espionage       => fromBaseCostPowerOf2(Resources(200, 1_000, 200), level)
      case Computer        => fromBaseCostPowerOf2(Resources(0, 400, 600), level)
      case Astrophysics    => fromBaseCostPowerOf2(Resources(4_000, 8_000, 4_000), level)
      case ResearchNetwork => fromBaseCostPowerOf2(Resources(240_000, 400_000, 160_000), level)
      case Graviton        => gravitonTechnologyCost(level)
      case Weapons         => fromBaseCostPowerOf2(Resources(800, 200, 0), level)
      case Shielding       => fromBaseCostPowerOf2(Resources(200, 600, 0), level)
      case Armor           => fromBaseCostPowerOf2(Resources(1_000, 0, 0), level)
    }
  }

  private def gravitonTechnologyCost(level: Int Refined Positive): Resources = {
    if (level.value != 1) throw new IllegalStateException("Graviton Technology should not be developed to higher levels.")
    Resources(0, 0, 0, 300_000)
  }

  private def fromBaseCostPowerOf2(baseCost: Resources, level: Int Refined Positive): Resources = {
    Resources(
      metal = (baseCost.metal * 2.0.pow(level.value - 1.0)).toInt,
      crystal = (baseCost.crystal * 2.0.pow(level.value - 1.0)).toInt,
      deuterium = (baseCost.deuterium * 2.0.pow(level.value - 1.0)).toInt
    )
  }

  private def refineVUnsafe[P, V](v: V)(implicit ev: Validate[V, P]): Refined[V, P] =
    refineV[P](v).fold(s => throw new IllegalArgumentException(s), identity)
}
