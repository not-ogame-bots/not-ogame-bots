package not.ogame.bots.ordon

import not.ogame.bots.Coordinates
import not.ogame.bots.CoordinatesType.Moon
import not.ogame.bots.ordon.utils.Stats

import scala.io.Source

class ScrapeOgameApiSpec extends munit.FunSuite {
  test("Scrape ogame api") {
    val playerToHonorPoints = getPlayerToHonorPoints
    val playerToMilitaryPoints = getPlayerToMilitaryPoints
    val playerToPlanetList = Stats.getPlayerToPlanetList
    val activePlayerNames = Stats.getPlayerToNameMap

    val potentialTargets = playerToMilitaryPoints.filter(_._2 > 2_000_000).keys.toSet
    val minimalTargets = playerToMilitaryPoints.filter(_._2 > 100_000).keys.toSet
    val bandits = playerToHonorPoints.filter(_._2 < -500).keys.toSet

    playerToPlanetList
      .filter(entry => activePlayerNames.contains(entry._1))
      .filter(entry => potentialTargets.contains(entry._1))
      //      .filter(entry => potentialTargets.contains(entry._1) || (bandits.contains(entry._1) && minimalTargets.contains(entry._1)))
      //      .filter(entry => bandits.contains(entry._1))
      //      .filter(entry => entry._2.size < 3 + entry._2.count(_.galaxy == 1))
      .map(entry => entry._1 -> sortCoordinates(entry._2))
      .toList
      .sortBy(entry => entry._2.count(_.coordinatesType == Moon))
      .foreach(entry => {
        println(playerToMilitaryPoints(entry._1) + " " + bandits.contains(entry._1))
        println(entry._2)
      })
    //100794
    //    playerToPlanetList("100794").foreach(println(_))
    //    getPlayerToPlanetList("101452").foreach(println(_))
  }

  private def sortCoordinates(value: Seq[Coordinates]): Seq[Coordinates] = {
    value.sortBy(c => c.galaxy * 100_000 + c.system * 100 + c.position)
  }

  private def getPlayerToMilitaryPoints: Map[String, Int] = {
    getXmlLines("https://s169-pl.ogame.gameforge.com/api/highscore.xml?category=1&type=3")
      .flatMap(_.split("><"))
      .filter(_.startsWith("player"))
      .map(row => row.split(" ")(2).drop(4).takeWhile(_.isDigit) -> row.split(" ")(3).drop(7).takeWhile(_.isDigit).toInt)
      .toMap
  }

  private def getPlayerToHonorPoints: Map[String, Int] = {
    getXmlLines("https://s169-pl.ogame.gameforge.com/api/highscore.xml?category=1&type=7")
      .flatMap(_.split("><"))
      .filter(_.startsWith("player"))
      .map(row => row.split("id=\"")(1).takeWhile(_.isDigit) -> row.split("score=\"")(1).takeWhile(c => c.isDigit || c == '-').toInt)
      .toMap
  }

  private def getXmlLines(url: String): Iterator[String] = {
    val source = Source.fromURL(url)
    val lines = source.getLines()
    lines
  }
}
