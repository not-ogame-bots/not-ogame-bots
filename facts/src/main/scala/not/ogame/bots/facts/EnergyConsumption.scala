package not.ogame.bots.facts

object EnergyConsumption {
  def metalEnergyConsumption(level: Int): Int = {
    (10 * level * Math.pow(1.1, level)).toInt
  }

  def crystalEnergyConsumption(level: Int): Int = {
    (10 * level * Math.pow(1.1, level)).toInt
  }

  def deuteriumEnergyConsumption(level: Int): Int = {
    (20 * level * Math.pow(1.1, level)).toInt
  }

  def solarPlantEnergyProduction(level: Int): Int = {
    (20 * level * Math.pow(1.1, level)).toInt
  }
}
