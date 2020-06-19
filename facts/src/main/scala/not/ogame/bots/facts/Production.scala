package not.ogame.bots.facts

object Production {
  def totalHourlyMetalProduction(metalMineLevel: Int): Int = 30 + metalMineHourlyProduction(metalMineLevel)

  def totalHourlyCrystalProduction(crystalMineLevel: Int): Int = 30 + crystalMineHourlyProduction(crystalMineLevel)

  def metalMineHourlyProduction(metalMineLevel: Int): Int = (30 * metalMineLevel * (1.1 pow metalMineLevel)).toInt

  def crystalMineHourlyProduction(crystalMineLevel: Int): Int = (20 * crystalMineLevel * (1.1 pow crystalMineLevel)).toInt

  //Average. For temperature = 0
  def deuteriumSynthesizerHourlyProduction(deuteriumSynthesizerLevel: Int): Int =
    (10 * deuteriumSynthesizerLevel * (1.1 pow deuteriumSynthesizerLevel) * 1.36).toInt
}
