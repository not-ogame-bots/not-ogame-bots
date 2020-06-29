package not.ogame.bots.ordon

import scala.io.Source

class ScrapeOgameApiSpec extends munit.FunSuite {
  test("Scrape ogame api") {
    val playerToMilitaryPoints = getPlayerToMilitaryPoints
    val potentialTargets = playerToMilitaryPoints.filter(_._2 > 200_000).keys.toSet
    val planetsOfPotentialTargets = getPlayerToPlanetList.filter(entry => potentialTargets.contains(entry._1))
    planetsOfPotentialTargets
      .filter(entry => entry._2.map(_._1).forall(_.startsWith("1")))
      .map(entry => entry._1 -> entry._2.sortBy(_._1.split(':')(1).takeWhile(_.isDigit).toInt))
      .foreach(entry => {
        println(playerToMilitaryPoints(entry._1))
        println(entry._2)
      })
    //    getPlayerToPlanetList("101452").foreach(println(_))
  }

  private def getPlayerToMilitaryPoints: Map[String, Int] = {
    Source
      .fromURL("https://s169-pl.ogame.gameforge.com/api/highscore.xml?category=1&type=3")
      .getLines()
      .flatMap(_.split("><"))
      .filter(_.startsWith("player"))
      .map(row => row.split(" ")(2).drop(4).takeWhile(_.isDigit) -> row.split(" ")(3).drop(7).takeWhile(_.isDigit).toInt)
      .toMap
  }

  private def getPlayerToPlanetList: Map[String, Seq[(String, Boolean)]] = {
    Source
      .fromURL("https://s169-pl.ogame.gameforge.com/api/universe.xml")
      .getLines()
      .flatMap(_.split("<planet "))
      .filter(_.startsWith("id"))
      .map(row => row.split("player=\"")(1).takeWhile(_ != '"') -> (row.split("coords=\"")(1).takeWhile(_ != '"') -> row.contains("moon")))
      .toSeq
      .groupMap(_._1)(_._2)
  }
}
