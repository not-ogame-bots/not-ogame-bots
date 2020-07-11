package not.ogame.bots.ordon.action

import java.time.ZonedDateTime

import not.ogame.bots.PlayerActivity.LessThan15MinutesAgo
import not.ogame.bots.ordon.core.{EventRegistry, OrdonOgameDriver, TimeBasedOrdonAction}
import not.ogame.bots.ordon.utils.{Galaxy, SlackIntegration}
import not.ogame.bots.{Coordinates, PlayerActivity, PlayerPlanet}

class MonitorActivityOrdonAction(monitoredPlayerName: String, playerPlanet: PlayerPlanet, coordinatesList: List[Coordinates])
    extends TimeBasedOrdonAction {
  private val slackIntegration = new SlackIntegration()

  override def processTimeBased(ogame: OrdonOgameDriver, eventRegistry: EventRegistry): ZonedDateTime = {
    val playerActivityMap = Galaxy.getPlayerActivityMap(ogame, playerPlanet, coordinatesList)
    val message = playerActivityMap.toList
      .sortBy(entry => entry._1.galaxy * 100000 + entry._1.system * 100 + entry._1.position)
      .map(entry => {
        entryToString(entry)
      })
      .mkString(s"$monitoredPlayerName\n", "\n", "\n")
    slackIntegration.postActivityMessageToSlack(message)
    ZonedDateTime.now().plusMinutes(10)
  }

  private def entryToString(entry: (Coordinates, PlayerActivity)) = {
    val active = if (entry._2 == LessThan15MinutesAgo) "\t\t\tActive" else entry._2.toString
    s"${entry._1.galaxy}:${entry._1.system}:${entry._1.position}\t${entry._1.coordinatesType}\t$active"
  }
}
