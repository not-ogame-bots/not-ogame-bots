package not.ogame.bots.ordon.utils

import not.ogame.bots.Coordinates
import not.ogame.bots.CoordinatesType.Moon

import scala.io.Source

object Stats {
  def getTargets: List[String] = {
    val playerToHonorPoints = getPlayerToHonorPoints
    val playerToMilitaryPoints = getPlayerToMilitaryPoints
    val playerToPlanetList = Stats.getPlayerToPlanetList
    val activePlayerNames = Stats.getPlayerToNameMap

    val potentialTargets = playerToMilitaryPoints.filter(_._2 > 500_000).keys.toSet
    val minimalTargets = playerToMilitaryPoints.filter(_._2 > 100_000).keys.toSet
    val bandits = playerToHonorPoints.filter(_._2 < -500).keys.toSet

    playerToPlanetList
      .filter(entry => activePlayerNames.contains(entry._1))
      .filter(entry => potentialTargets.contains(entry._1) || (bandits.contains(entry._1) && minimalTargets.contains(entry._1)))
      .filter(entry => entry._2.size > 11)
      .filter(entry => entry._2.size < 3 + entry._2.count(_.galaxy == 1))
      .keys
      .toList
  }

  def getPlayerToMilitaryPoints: Map[String, Int] = {
    getXmlLines("https://s169-pl.ogame.gameforge.com/api/highscore.xml?category=1&type=3")
      .flatMap(_.split("><"))
      .filter(_.startsWith("player"))
      .map(row => row.split(" ")(2).drop(4).takeWhile(_.isDigit) -> row.split(" ")(3).drop(7).takeWhile(_.isDigit).toInt)
      .toMap
  }

  def getPlayerToHonorPoints: Map[String, Int] = {
    getXmlLines("https://s169-pl.ogame.gameforge.com/api/highscore.xml?category=1&type=7")
      .flatMap(_.split("><"))
      .filter(_.startsWith("player"))
      .map(row => row.split("id=\"")(1).takeWhile(_.isDigit) -> row.split("score=\"")(1).takeWhile(c => c.isDigit || c == '-').toInt)
      .toMap
  }

  def getPlayerToNameMap: Map[String, String] = {
    getXmlLines("https://s169-pl.ogame.gameforge.com/api/players.xml")
      .flatMap(_.split("<player "))
      .filter(_.startsWith("id"))
      .filter(row => !getAttribute(row, "status").contains("a"))
      .filter(row => !getAttribute(row, "status").contains("v"))
      .map(row => getAttribute(row, "id") -> getAttribute(row, "name"))
      .toMap
  }

  def getPlayerToPlanetList: Map[String, List[Coordinates]] = {
    getXmlLines("https://s169-pl.ogame.gameforge.com/api/universe.xml")
      .flatMap(_.split("<planet "))
      .filter(_.startsWith("id"))
      .flatMap(row => {
        val playerId = getAttribute(row, "player")
        val coordinatesParts = getAttribute(row, "coords").split(":")
        val coordinates = Coordinates(coordinatesParts(0).toInt, coordinatesParts(1).toInt, coordinatesParts(2).toInt)
        if (row.contains("moon")) {
          List(playerId -> coordinates, playerId -> coordinates.copy(coordinatesType = Moon))
        } else {
          List(playerId -> coordinates)
        }
      })
      .toSeq
      .groupMap(_._1)(_._2)
      .map(e => e._1 -> e._2.toList)
  }

  private def getAttribute(row: String, name: String): String = {
    val split = row.split(name + "=\"")
    if (split.size == 1) {
      ""
    } else {
      split(1).takeWhile(_ != '"')
    }
  }

  private def getXmlLines(url: String): Iterator[String] = {
    val source = Source.fromURL(url)
    val lines = source.getLines()
    lines
  }
}
