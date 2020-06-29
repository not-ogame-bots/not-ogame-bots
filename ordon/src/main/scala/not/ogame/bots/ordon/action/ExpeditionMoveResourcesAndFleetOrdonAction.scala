package not.ogame.bots.ordon.action

import java.time.ZonedDateTime

import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots.ShipType.{Destroyer, EspionageProbe, Explorer, LargeCargoShip}
import not.ogame.bots.ordon.core.{OrdonOgameDriver, TimeBasedOrdonAction}
import not.ogame.bots.ordon.utils.{FleetSelector, ResourceSelector, Selector, SendFleet}
import not.ogame.bots.{FleetSpeed, MyFleet, PlayerPlanet, ShipType}

class ExpeditionMoveResourcesAndFleetOrdonAction(planet: PlayerPlanet, moon: PlayerPlanet, expeditionFleet: Map[ShipType, Int])
    extends TimeBasedOrdonAction {
  val fromPlanetToMoon = new SendFleet(
    from = planet,
    to = moon,
    selectResources =
      new ResourceSelector(metalSelector = Selector.skip, crystalSelector = Selector.skip, deuteriumSelector = Selector.atMost(300_000)),
    selectShips = page => Map(LargeCargoShip -> page.ships(LargeCargoShip)),
    fleetSpeed = FleetSpeed.Percent10
  )
  val fromMoonToPlanet = new SendFleet(
    from = moon,
    to = planet,
    selectResources = new ResourceSelector(deuteriumSelector = Selector.decreaseBy(300_000)),
    selectShips = new FleetSelector(
      filters = Map(
        Destroyer -> Selector.decreaseBy(3),
        EspionageProbe -> Selector.decreaseBy(50),
        Explorer -> Selector.skip,
        LargeCargoShip -> Selector.decreaseBy(expeditionFleet(LargeCargoShip))
      )
    ),
    fleetSpeed = FleetSpeed.Percent100
  )

  override def processTimeBased(ogame: OrdonOgameDriver): ZonedDateTime = {
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
    fleet.ships(LargeCargoShip) > 0
  }

  private def isFlyingOnCorrectPath(fleet: MyFleet) = {
    (fleet.from == planet.coordinates && fleet.to == moon.coordinates) ||
    (fleet.from == moon.coordinates && fleet.to == planet.coordinates)
  }

  private def sendFleet(ogame: OrdonOgameDriver): Unit = {
    val isFleetOnPlanet = ogame.readFleetPage(planet.id).ships(LargeCargoShip) > 0
    if (isFleetOnPlanet)
      ogame.sendFleet(fromPlanetToMoon.getSendFleetRequest(ogame))
    else {
      ogame.sendFleet(fromMoonToPlanet.getSendFleetRequest(ogame))
    }
  }
}
