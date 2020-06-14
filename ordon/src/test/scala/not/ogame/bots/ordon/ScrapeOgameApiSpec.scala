package not.ogame.bots.ordon

import scala.io.Source

class ScrapeOgameApiSpec extends munit.FunSuite {
  test("Scrape ogame api") {
    val potentialTargets = getPlayerToMilitaryPoints.filter(_._2 > 500_000).keys.toSet
    val planetsOfPotentialTargets = getPlaterToPlanetList.filter(entry => potentialTargets.contains(entry._1))
    planetsOfPotentialTargets
      .filter(entry => entry._2.count(_._2) == 1)
      .foreach(println(_))
  }

  private def getPlayerToMilitaryPoints: Map[String, Int] = {
    Source
      .fromURL("https://s168-pl.ogame.gameforge.com/api/highscore.xml?category=1&type=3")
      .getLines()
      .flatMap(_.split("><"))
      .filter(_.startsWith("player"))
      .map(row => row.split(" ")(2).drop(4).takeWhile(_.isDigit) -> row.split(" ")(3).drop(7).takeWhile(_.isDigit).toInt)
      .toMap
  }

  private def getPlaterToPlanetList: Map[String, Seq[(String, Boolean)]] = {
    Source
      .fromURL("https://s168-pl.ogame.gameforge.com/api/universe.xml")
      .getLines()
      .flatMap(_.split("<planet "))
      .filter(_.startsWith("id"))
      .map(row => row.split("player=\"")(1).takeWhile(_ != '"') -> (row.split("coords=\"")(1).takeWhile(_ != '"') -> row.contains("moon")))
      .toSeq
      .groupMap(_._1)(_._2)
  }
}
