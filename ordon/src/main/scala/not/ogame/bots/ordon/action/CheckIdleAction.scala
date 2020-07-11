package not.ogame.bots.ordon.action

import java.time.ZonedDateTime

import not.ogame.bots.PlayerActivity.LessThan15MinutesAgo
import not.ogame.bots.ordon.core._
import not.ogame.bots.ordon.utils.{Galaxy, Stats}
import not.ogame.bots.{Coordinates, PlayerActivity, PlayerPlanet}

class CheckIdleAction(planet: PlayerPlanet, playersIdOrName: Iterator[String]) extends BaseOrdonAction {
  override def shouldHandleEvent(event: OrdonEvent): Boolean = true

  override def doProcess(event: OrdonEvent, ogame: OrdonOgameDriver, eventRegistry: EventRegistry): List[OrdonAction] = {
    checkIdle(ogame)
    eventRegistry.registerEvent(TimeBasedOrdonEvent(ZonedDateTime.now().plusMinutes(10)))
    List()
  }

  private def checkIdle(ogame: OrdonOgameDriver): Unit = {
    val playerToNameMap = Stats.getPlayerToNameMap
    val playerToPlanetList = Stats.getPlayerToPlanetList
    playersIdOrName.foreach(
      idOrName =>
        if (playerToNameMap.contains(idOrName)) {
          printPlayerStatusById(ogame, playerToNameMap, playerToPlanetList, idOrName)
        } else {
          playerToNameMap
            .find(e => e._2 == idOrName)
            .foreach(e => printPlayerStatusById(ogame, playerToNameMap, playerToPlanetList, e._1))
        }
    )
  }

  private def printPlayerStatusById(
      ogame: OrdonOgameDriver,
      playerToNameMap: Map[String, String],
      playerToPlanetList: Map[String, List[Coordinates]],
      idOrName: String
  ): Unit = {
    val playerActivityMap = Galaxy.getPlayerActivityMap(ogame, planet, playerToPlanetList(idOrName))
    val message = playerActivityMap.toList
      .sortBy(entry => entry._1.galaxy * 100000 + entry._1.system * 100 + entry._1.position)
      .map(entry => {
        entryToString(entry)
      })
      .mkString(s"${playerToNameMap(idOrName)}\n", "\n", "\n")
    println(message)
  }

  private def entryToString(entry: (Coordinates, PlayerActivity)) = {
    val active = if (entry._2 == LessThan15MinutesAgo) "\t\t\tActive" else entry._2.toString
    s"${entry._1.galaxy}:${entry._1.system}:${entry._1.position}\t${entry._1.coordinatesType}\t$active"
  }
}
