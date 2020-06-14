package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots.ShipType.SmallCargoShip
import not.ogame.bots._

import scala.util.Random

class FsBattleOgameAction[T[_]: Monad](
    planet: PlayerPlanet,
    moon: PlayerPlanet,
    expeditionMoon: PlayerPlanet,
    otherMoon: PlayerPlanet,
    safeBufferInMinutes: Int = 5,
    randomUpperLimitInSeconds: Int = 120
)(implicit clock: LocalClock)
    extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      allMyFleets <- ogame.readMyFleets()
      thisMyFleet = allMyFleets.fleets.find(isThisMyFleet)
      resumeOn <- processMyFleet(ogame, thisMyFleet)
    } yield resumeOn

  private def isThisMyFleet(fleet: MyFleet): Boolean = {
    fleet.from == planet.coordinates && fleet.to == moon.coordinates && fleet.fleetMissionType == Deployment
  }

  private def processMyFleet(ogame: OgameDriver[T], thisMyFleet: Option[MyFleet]): T[ZonedDateTime] = {
    thisMyFleet match {
      case Some(fleet) if !fleet.isReturning => returnOrWait(ogame, fleet)
      case Some(fleet) if fleet.isReturning  => scheduleSend(fleet)
      case None                              => send(ogame)
    }
  }

  private def returnOrWait(ogame: OgameDriver[T], fleet: MyFleet): T[ZonedDateTime] = {
    if (isCloseToArrival(fleet)) {
      ogame.returnFleet(fleet.fleetId).map(_ => clock.now())
    } else {
      chooseTimeWhenClickReturn(fleet).pure[T]
    }
  }

  private def isCloseToArrival(fleet: MyFleet) = {
    fleet.arrivalTime.minusMinutes(safeBufferInMinutes).minusSeconds(randomUpperLimitInSeconds).isBefore(clock.now())
  }

  private def chooseTimeWhenClickReturn(fleet: MyFleet): ZonedDateTime = {
    fleet.arrivalTime.minusMinutes(safeBufferInMinutes).minusSeconds(Random.nextLong(randomUpperLimitInSeconds))
  }

  private def scheduleSend(fleet: MyFleet): T[ZonedDateTime] = {
    fleet.arrivalTime.plusSeconds(3).pure[T]
  }

  private def send(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      myFleets <- ogame.readMyFleets()
      smallCargoFlying = countSmallCargoFlyingInCargoFleet(myFleets.fleets)
      smallCargoOnExpeditionMoon <- ogame.readFleetPage(expeditionMoon.id).map(_.ships(SmallCargoShip))
      smallCargoOnOtherMoon <- ogame.readFleetPage(otherMoon.id).map(_.ships(SmallCargoShip))
      totalSmallCargo = smallCargoFlying + smallCargoOnExpeditionMoon + smallCargoOnOtherMoon
      _ <- handleAmountOfSmallCargoInCargoFleet(ogame, totalSmallCargo)
    } yield clock.now()

  private def countSmallCargoFlyingInCargoFleet(myFleets: List[MyFleet]): Int = {
    myFleets
      .filter(fleet => contributeToCargoFleet(fleet))
      .map(fleet => fleet.ships(SmallCargoShip))
      .sum
  }

  private def contributeToCargoFleet(fleet: MyFleet): Boolean = {
    isCargoFleet(fleet) || isFlyingToExpeditionMoon(fleet)
  }

  private def isCargoFleet(fleet: MyFleet): Boolean = {
    Set(fleet.from, fleet.to) == Set(expeditionMoon.coordinates, otherMoon.coordinates) && fleet.fleetMissionType == Deployment
  }

  private def isFlyingToExpeditionMoon(fleet: MyFleet): Boolean = {
    fleet.from == planet.coordinates && fleet.to == expeditionMoon.coordinates && fleet.fleetMissionType == Deployment
  }

  private def handleAmountOfSmallCargoInCargoFleet(ogame: OgameDriver[T], totalSmallCargo: Int): T[Unit] = {
    if (totalSmallCargo < 20_000) {
      ogame
        .sendFleet(
          SendFleetRequest(
            from = planet,
            ships = SendFleetRequestShips.Ships(Map((SmallCargoShip, 20_000 - totalSmallCargo))),
            targetCoordinates = expeditionMoon.coordinates,
            fleetMissionType = Deployment,
            resources = FleetResources.Given(Resources(0, 0, 0)),
            speed = FleetSpeed.Percent100
          )
        )
    } else {
      ogame
        .sendFleet(
          SendFleetRequest(
            from = planet,
            ships = SendFleetRequestShips.AllShips,
            targetCoordinates = moon.coordinates,
            fleetMissionType = Deployment,
            resources = FleetResources.Max,
            speed = FleetSpeed.Percent10
          )
        )
    }
  }
}
