package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots._

import scala.util.Random

class DeployAndReturnOgameAction[T[_]: Monad](
    planet: PlayerPlanet,
    moon: PlayerPlanet
)(implicit clock: LocalClock)
    extends SimpleOgameAction[T] {
  private val safeBufferInMinutes = 2
  private val randomUpperLimitInSeconds = 120

  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      allMyFleets <- ogame.readMyFleets()
      thisMyFleet = allMyFleets.find(isThisMyFleet)
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

  private def send(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    val fleetSpeed = Random.shuffle(List(FleetSpeed.Percent10, FleetSpeed.Percent20)).head
    ogame
      .sendFleet(
        SendFleetRequest(
          from = planet,
          ships = SendFleetRequestShips.AllShips,
          targetCoordinates = moon.coordinates,
          fleetMissionType = Deployment,
          resources = FleetResources.Max,
          speed = fleetSpeed
        )
      )
      .map(_ => clock.now())
  }
}
