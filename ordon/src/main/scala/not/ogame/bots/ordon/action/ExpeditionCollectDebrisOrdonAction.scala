package not.ogame.bots.ordon.action

import not.ogame.bots.CoordinatesType.Debris
import not.ogame.bots.FleetMissionType.Recycle
import not.ogame.bots.FleetSpeed.Percent100
import not.ogame.bots.SendFleetRequestShips.Ships
import not.ogame.bots.ShipType.Explorer
import not.ogame.bots.ordon.core._
import not.ogame.bots.ordon.utils.SendFleet
import not.ogame.bots.{Coordinates, PlanetId, PlayerPlanet, Resources}

class ExpeditionCollectDebrisOrdonAction(expeditionPlanet: PlayerPlanet) extends BaseOrdonAction {
  override def shouldHandleEvent(event: OrdonEvent): Boolean = {
    event.isInstanceOf[ExpeditionFleetChanged]
  }

  override def doProcess(event: OrdonEvent, ogame: OrdonOgameDriver, eventRegistry: EventRegistry): List[OrdonAction] = {
    handleExpeditionChangeEvent(ogame, event.asInstanceOf[ExpeditionFleetChanged])
    List(this)
  }

  private def handleExpeditionChangeEvent(ogame: OrdonOgameDriver, expeditionFleetChanged: ExpeditionFleetChanged): Unit = {
    val expeditionDebrisTarget = expeditionFleetChanged.to.copy(coordinatesType = Debris)
    val galaxyPageData = ogame.readGalaxyPage(expeditionPlanet.id, expeditionDebrisTarget.galaxy, expeditionDebrisTarget.system)
    val myFleetPageData = ogame.readMyFleets()
    val isAlreadyCollectingDebris = myFleetPageData.fleets.exists(
      fleet => fleet.from == expeditionPlanet.coordinates && fleet.to == expeditionDebrisTarget && !fleet.isReturning
    )
    val isThereDebrisToCollect = galaxyPageData.debrisMap.contains(expeditionDebrisTarget)
    if (!isAlreadyCollectingDebris && isThereDebrisToCollect) {
      collectDebris(ogame, expeditionDebrisTarget)
    }
  }

  private def collectDebris(ogame: OrdonOgameDriver, expeditionDebrisTarget: Coordinates): Unit = {
    val sendFleetRequest = new SendFleet(
      from = expeditionPlanet,
      to = PlayerPlanet(PlanetId.apply(""), expeditionDebrisTarget),
      selectResources = _ => Resources.Zero,
      selectShips = page => Map(Explorer -> Math.min(page.ships(Explorer), 300)),
      fleetSpeed = Percent100,
      missionType = Recycle
    ).getSendFleetRequest(ogame)
    if (sendFleetRequest.ships.asInstanceOf[Ships].ships.values.sum > 0) {
      ogame.sendFleet(sendFleetRequest)
    }
  }
}
