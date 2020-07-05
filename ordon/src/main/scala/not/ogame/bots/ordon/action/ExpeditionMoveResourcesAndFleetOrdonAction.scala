package not.ogame.bots.ordon.action

import java.time.ZonedDateTime

import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots.ShipType._
import not.ogame.bots.ordon.core.{EventRegistry, OrdonOgameDriver, TimeBasedOrdonAction}
import not.ogame.bots.ordon.utils.{FleetSelector, ResourceSelector, Selector, SendFleet}
import not.ogame.bots.{FleetSpeed, MyFleet, PlayerPlanet, ShipType}

class ExpeditionMoveResourcesAndFleetOrdonAction(planet: PlayerPlanet, moon: PlayerPlanet, expeditionFleet: Map[ShipType, Int])
    extends TimeBasedOrdonAction {
  val fromPlanetToMoon = new SendFleet(
    from = planet,
    to = moon,
    selectResources =
      new ResourceSelector(metalSelector = Selector.skip, crystalSelector = Selector.skip, deuteriumSelector = Selector.atMost(300_000)),
    selectShips = page => Map(Explorer -> page.ships(Explorer)),
    fleetSpeed = FleetSpeed.Percent10
  )
  val fromMoonToPlanet = new SendFleet(
    from = moon,
    to = planet,
    selectResources = new ResourceSelector(deuteriumSelector = Selector.decreaseBy(300_000)),
    selectShips = new FleetSelector(
      filters = Map(
        LightFighter -> Selector.skip,
        LargeCargoShip -> Selector.skip,
        Destroyer -> Selector.decreaseBy(3),
        EspionageProbe -> Selector.decreaseBy(50),
        Explorer -> Selector.decreaseBy(600)
      )
    ),
    fleetSpeed = FleetSpeed.Percent100
  )

  override def processTimeBased(ogame: OrdonOgameDriver, eventRegistry: EventRegistry): ZonedDateTime = {
    val maybeThisFleet = ogame.readMyFleets().fleets.find(isThisFleet)
    if (maybeThisFleet.isDefined) {
      maybeThisFleet.get.arrivalTime.plusSeconds(3)
    } else {
      sendFleet(ogame)
      ogame.readMyFleets().fleets.find(isThisFleet).get.arrivalTime.plusSeconds(3)
    }
  }

  private def isThisFleet(fleet: MyFleet): Boolean = {
    isFlyingOnCorrectPath(fleet) &&
    fleet.fleetMissionType == Deployment &&
    fleet.ships(Explorer) > 0
  }

  private def isFlyingOnCorrectPath(fleet: MyFleet) = {
    (fleet.from == planet.coordinates && fleet.to == moon.coordinates) ||
    (fleet.from == moon.coordinates && fleet.to == planet.coordinates)
  }

  private def sendFleet(ogame: OrdonOgameDriver): Unit = {
    val isFleetOnPlanet = ogame.readFleetPage(planet.id).ships(Explorer) > 0
    if (isFleetOnPlanet)
      ogame.sendFleet(fromPlanetToMoon.getSendFleetRequest(ogame))
    else {
      ogame.sendFleet(fromMoonToPlanet.getSendFleetRequest(ogame))
    }
  }

  override def toString: String = s"Move resources $resumeOn"
}
