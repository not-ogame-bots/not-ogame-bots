package not.ogame.bots.ordon.utils

import not.ogame.bots.ordon.core.OrdonOgameDriver
import not.ogame.bots.{Coordinates, PlayerActivity, PlayerPlanet}

object Galaxy {
  def getPlayerActivityMap(
      ogame: OrdonOgameDriver,
      playerPlanet: PlayerPlanet,
      coordinatesList: List[Coordinates]
  ): Map[Coordinates, PlayerActivity] = {
    coordinatesList
      .groupBy(c => c.galaxy -> c.system)
      .flatMap(entry => {
        val g = entry._1._1
        val s = entry._1._2
        val cList = entry._2
        val galaxyPageData = ogame.readGalaxyPage(playerPlanet.id, g, s)
        galaxyPageData.playerActivityMap.filter(entry => cList.contains(entry._1))
      })
  }
}
