package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots.ShipType.{Destroyer, EspionageProbe, Explorer, LargeCargoShip}
import not.ogame.bots._
import not.ogame.bots.ordon.utils._

class FlyAroundWithLargeCargoOgameAction[T[_]: Monad](
    planet: PlayerPlanet,
    moon: PlayerPlanet
)(implicit clock: LocalClock)
    extends SimpleOgameAction[T] {
  val fromPlanetToMoon = new SendFleet(
    from = planet,
    to = moon,
    selectResources = _ => Resources(0, 0, 0),
    selectShips = page => Map(LargeCargoShip -> page.ships(LargeCargoShip)),
    fleetSpeed = FleetSpeed.Percent10
  )
  val fromMoonToPlanet = new SendFleet(
    from = moon,
    to = planet,
    selectResources = new ResourceSelector(deuteriumSelector = Selector.decreaseBy(300_000)),
    selectShips = new FleetSelector(
      filters = Map(
        Destroyer -> Selector.decreaseBy(6),
        EspionageProbe -> Selector.decreaseBy(50),
        Explorer -> Selector.skip,
        LargeCargoShip -> Selector.decreaseBy(410)
      )
    ),
    fleetSpeed = FleetSpeed.Percent10
  )

  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    ogame.readMyFleets().flatMap(fleets => findThisFleet(fleets.fleets).map(getResumeOnForFleet).getOrElse(sendFleet(ogame)))
  }

  private def findThisFleet(fleets: List[MyFleet]): Option[MyFleet] = {
    fleets.find(isThisFleet)
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

  private def getResumeOnForFleet(fleet: MyFleet): T[ZonedDateTime] = {
    // There is an issue in ogame that fleet just after arrival is still on fleet list as returning. To avoid that 3 second delay was added.
    fleet.arrivalTime.plusSeconds(3).pure[T]
  }

  private def sendFleet(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      shipsOnPlanet <- ogame.readFleetPage(planet.id)
      sendFormPlanet = shipsOnPlanet.ships(LargeCargoShip) > 0
      arrivalTime <- if (sendFormPlanet) fromPlanetToMoon.sendDeployment(ogame) else fromMoonToPlanet.sendDeployment(ogame)
    } yield arrivalTime
}
