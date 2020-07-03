package not.ogame.bots.ordon.action

import java.time.ZonedDateTime

import not.ogame.bots.CoordinatesType.Planet
import not.ogame.bots.FleetMissionType.Expedition
import not.ogame.bots.ShipType._
import not.ogame.bots._
import not.ogame.bots.ordon.core.{EventRegistry, ExpeditionFleetChanged, OrdonOgameDriver, TimeBasedOrdonAction}

class ExpeditionOrdonAction(val startPlanet: PlayerPlanet) extends TimeBasedOrdonAction {
  private val expeditionFleetHelper = new ExpeditionFleetHelper()

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
      ships = SendFleetRequestShips.Ships(ships = expeditionFleetHelper.getExpeditionFleet(ogame, startPlanet))
    )
  }

  override def toString: String = s"Expedition $resumeOn"
}

class ExpeditionFleetHelper {
  private val maxLargeCargoCount = 1100
  private val masExplorerCount = 100

  def getExpeditionFleet(ogame: OrdonOgameDriver, startPlanet: PlayerPlanet): Map[ShipType, Int] = {
    val myFleetPageData = ogame.readMyFleets()
    val fleetPageData = ogame.readFleetPage(startPlanet.id)
    val availableExpeditionShips = (myFleetPageData.fleets
      .filter(f => f.fleetMissionType == Expedition)
      .flatMap(_.ships.toList) ++ fleetPageData.ships.toList)
      .groupBy(e => e._1)
      .map(e => e._1 -> e._2.map(_._2).sum)
    val averageExpeditionShips = availableExpeditionShips
      .map(e => e._1 -> e._2 / myFleetPageData.fleetSlots.maxExpeditions)
    Map(
      LightFighter -> averageExpeditionShips(LightFighter),
      LargeCargoShip -> Math.min(averageExpeditionShips(LargeCargoShip), maxLargeCargoCount),
      Explorer -> Math.min(averageExpeditionShips(Explorer), masExplorerCount),
      Destroyer -> 1,
      EspionageProbe -> 1
    )
  }
}
