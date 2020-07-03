package not.ogame.bots.ordon.action

import java.time.ZonedDateTime

import not.ogame.bots.CoordinatesType.Planet
import not.ogame.bots.FleetMissionType.Expedition
import not.ogame.bots._
import not.ogame.bots.ordon.core.{EventRegistry, ExpeditionFleetChanged, OrdonOgameDriver, TimeBasedOrdonAction}
import not.ogame.bots.ordon.utils.SelectShips

class ExpeditionOrdonAction(val startPlanet: PlayerPlanet, val expeditionFleet: Map[ShipType, Int]) extends TimeBasedOrdonAction {
  private val selectShips: SelectShips = page => page.ships.map(e => e._1 -> Math.min(e._2, expeditionFleet.getOrElse(e._1, 0)))

  override def processTimeBased(ogame: OrdonOgameDriver, eventRegistry: EventRegistry): ZonedDateTime = {
    val page = ogame.readMyFleets()
    if (page.fleetSlots.currentExpeditions >= page.fleetSlots.maxExpeditions) {
      val nextExpeditionFleet = page.fleets.filter(_.fleetMissionType == Expedition).minBy(_.arrivalTime)
      eventRegistry.registerEvent(ExpeditionFleetChanged(nextExpeditionFleet.arrivalTime.plusSeconds(3), nextExpeditionFleet.to))
      nextExpeditionFleet.arrivalTime
    } else {
      ogame.sendFleet(getSendFleetRequest(ogame))
      processTimeBased(ogame, eventRegistry)
    }
  }

  private def getSendFleetRequest(ogame: OrdonOgameDriver): SendFleetRequest = {
    SendFleetRequest(
      from = startPlanet,
      targetCoordinates = startPlanet.coordinates.copy(position = 16, coordinatesType = Planet),
      fleetMissionType = Expedition,
      speed = FleetSpeed.Percent100,
      resources = FleetResources.Given(Resources.Zero),
      ships = SendFleetRequestShips.Ships(ships = getExpeditionFleet(ogame))
    )
  }

  def getExpeditionFleet(ogame: OrdonOgameDriver): Map[ShipType, Int] = selectShips(ogame.readFleetPage(startPlanet.id))

  override def toString: String = s"Expedition $resumeOn"
}
