package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots.ShipType.LargeCargoShip
import not.ogame.bots._
import not.ogame.bots.ordon.utils._

import scala.util.Random

class DeployAndReturnNoLargeCargoOgameAction[T[_]: Monad](
    planet: PlayerPlanet,
    moon: PlayerPlanet,
    expectedOffers: List[MyOffer] = List(),
    safeBufferInMinutes: Int = 30,
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
    fleet.from == planet.coordinates && fleet.to == moon.coordinates && fleet.fleetMissionType == Deployment && fleet.ships(LargeCargoShip) == 0
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
    fleet.arrivalTime.plusSeconds(40).pure[T]
  }

  private def send(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      _ <- new PutOffersToMarket().putOffersToMarket[T](ogame, planet, expectedOffers)
      fleetSelector = new FleetSelector(filters = Map(LargeCargoShip -> Selector.skip))
      resourceSelector = new ResourceSelector(deuteriumSelector = Selector.decreaseBy(300_000))
      _ <- new SendFleet(
        from = planet,
        to = moon,
        selectShips = fleetSelector,
        selectResources = resourceSelector,
        fleetSpeed = FleetSpeed.Percent10
      ).sendFleet(ogame)
        .map(_ => clock.now())
    } yield clock.now()
}
