package not.ogame.bots.ordon.action

import java.time.ZonedDateTime

import not.ogame.bots.FleetMissionType.Transport
import not.ogame.bots.FleetSpeed.Percent100
import not.ogame.bots.ordon.core.{EventRegistry, OrdonOgameDriver, TimeBasedOrdonAction}
import not.ogame.bots.ordon.utils.SendFleet
import not.ogame.bots.{MyFleet, PlayerPlanet}

import scala.util.Random

class TransportToOrdonAction(fromList: List[PlayerPlanet], to: PlayerPlanet) extends TimeBasedOrdonAction {
  override def processTimeBased(ogame: OrdonOgameDriver, eventRegistry: EventRegistry): ZonedDateTime = {
    val maybeFleet = findTransportFleet(ogame)
    if (maybeFleet.isDefined) {
      maybeFleet.get.arrivalTime.plusSeconds(3)
    } else {
      val maybeFrom = findFrom(ogame)
      if (maybeFrom.isDefined) {
        sendTransport(ogame, maybeFrom.get)
        findTransportFleet(ogame).get.arrivalTime.plusSeconds(3)
      } else {
        ZonedDateTime.now().plusHours(1)
      }
    }
  }

  private def findTransportFleet(ogame: OrdonOgameDriver): Option[MyFleet] = {
    ogame.readMyFleets().fleets.find(fleet => fleet.fleetMissionType == Transport && fleet.to == to.coordinates)
  }

  private def findFrom(ogame: OrdonOgameDriver): Option[PlayerPlanet] = {
    Random
      .shuffle(fromList)
      .find(from => {
        val currentResources = ogame.readSuppliesPage(from.id).currentResources
        val totalResources = currentResources.metal + currentResources.crystal + currentResources.deuterium
        totalResources > 2_000_000
      })
  }

  private def sendTransport(ogame: OrdonOgameDriver, from: PlayerPlanet): Unit = {
    ogame.sendFleet(
      new SendFleet(
        from = from,
        to = to,
        selectResources = page => page.currentResources,
        selectShips = page => page.ships,
        fleetSpeed = Percent100,
        missionType = Transport
      ).getSendFleetRequest(ogame)
    )
  }

  override def toString: String = s"Collect resources $resumeOn"
}
