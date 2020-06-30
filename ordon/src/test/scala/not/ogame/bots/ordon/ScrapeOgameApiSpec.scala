package not.ogame.bots.ordon

import scala.io.Source

class ScrapeOgameApiSpec extends munit.FunSuite {
  test("Scrape ogame api") {
    val playerToHonorPoints = getPlayerToHonorPoints
    val playerToMilitaryPoints = getPlayerToMilitaryPoints
    val playerToPlanetList = getPlayerToPlanetList

    val potentialTargets = playerToMilitaryPoints.filter(_._2 > 200_000).keys.toSet
    val bandits = playerToHonorPoints.filter(_._2 < -500).keys.toSet

    playerToPlanetList
      .filter(entry => potentialTargets.contains(entry._1))
      .filter(entry => bandits.contains(entry._1))
      //      .filter(entry => entry._2.map(_._1).forall(_.startsWith("1")))
      .map(entry => entry._1 -> sortCoordinates(entry._2))
      .foreach(entry => {
        println(playerToMilitaryPoints(entry._1))
        println(entry._2)
      })
    //    getPlayerToPlanetList("101452").foreach(println(_))
  }

  private def sortCoordinates(value: Seq[(String, Boolean)]): Seq[(String, Boolean)] = {
    value.sortBy(_._1.split(':').map(_.toInt).fold(1) { (a, b) =>
      a * 1000 + b
    })
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

  private def getPlayerToPlanetList: Map[String, Seq[(String, Boolean)]] = {
    getXmlLines("https://s169-pl.ogame.gameforge.com/api/universe.xml")
      .flatMap(_.split("<planet "))
      .filter(_.startsWith("id"))
      .map(row => row.split("player=\"")(1).takeWhile(_ != '"') -> (row.split("coords=\"")(1).takeWhile(_ != '"') -> row.contains("moon")))
      .toSeq
      .groupMap(_._1)(_._2)
  }

  private def getXmlLines(url: String): Iterator[String] = {
    val source = Source.fromURL(url)
    val lines = source.getLines()
    lines
  }
}
