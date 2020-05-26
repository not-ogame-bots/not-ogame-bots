package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots.FleetSpeed.Percent30
import not.ogame.bots.SendFleetRequestShips.Ships
import not.ogame.bots._

import scala.util.Random

class FlyAroundOgameAction[T[_]: Monad](
    targets: List[PlayerPlanet],
    fleetSelector: FleetSelector[T],
    resourceSelector: ResourceSelector[T]
)(implicit clock: LocalClock)
    extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    ogame.readAllFleets().flatMap(fleets => findThisFleet(fleets).map(getResumeOnForFleet).getOrElse(sendFleet(ogame)))
  }

  private def findThisFleet(fleets: List[Fleet]): Option[Fleet] = {
    fleets.find(isThisFleet)
  }

  private def isThisFleet(fleet: Fleet): Boolean = {
    targets.map(_.coordinates).contains(fleet.from) &&
    targets.map(_.coordinates).contains(fleet.to) &&
    fleet.fleetMissionType == Deployment
  }

  private def getResumeOnForFleet(fleet: Fleet): T[ZonedDateTime] = {
    // There is an issue in ogame that fleet just after arrival is still on fleet list as returning. In such case we need to just try again.
    if (fleet.isReturning) {
      clock.now().plusSeconds(2).pure[T]
    } else {
      fleet.arrivalTime.pure[T]
    }
  }

  private def sendFleet(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      planetToShips <- targets.map(it => fleetSelector.selectShips(ogame, it).map(it -> _)).sequence
      (startPlanet, ships) = planetToShips.maxBy(it => it._2.values.sum)
      targetPlanet = Random.shuffle(targets.filter(_ != startPlanet)).head
      resources <- resourceSelector.selectResources(ogame, startPlanet)
      _ <- ogame.sendFleet(
        SendFleetRequest(
          startPlanet,
          ships = Ships(ships),
          targetCoordinates = targetPlanet.coordinates,
          fleetMissionType = Deployment,
          resources = FleetResources.Given(resources),
          speed = Percent30
        )
      )
      now = clock.now()
    } yield now
}

class FleetSelector[T[_]: Monad](filters: Map[ShipType, Int => Int] = Map()) {
  def selectShips(ogameDriver: OgameDriver[T], playerPlanet: PlayerPlanet): T[Map[ShipType, Int]] = {
    ogameDriver.checkFleetOnPlanet(playerPlanet.id).map(shipsOnPlanet => computeShipsToSend(shipsOnPlanet))
  }

  private def computeShipsToSend(shipsOnPlanet: Map[ShipType, Int]): Map[ShipType, Int] = {
    ShipType.values.map(shipType => shipType -> computeShipToSend(shipType, shipsOnPlanet(shipType))).toMap.filter {
      case (_, count) => count > 0
    }
  }

  private def computeShipToSend(shipType: ShipType, countOnPlanet: Int): Int = {
    if (filters.contains(shipType)) {
      filters(shipType)(countOnPlanet)
    } else {
      countOnPlanet
    }
  }
}

class ResourceSelector[T[_]: Monad](
    metalSelector: Int => Int = Selector.all,
    crystalSelector: Int => Int = Selector.all,
    deuteriumSelector: Int => Int = Selector.all
) {
  def selectResources(ogameDriver: OgameDriver[T], playerPlanet: PlayerPlanet): T[Resources] = {
    ogameDriver
      .readSuppliesPage(playerPlanet.id)
      .map(_.currentResources)
      .map(
        it =>
          Resources(
            metal = metalSelector(it.metal),
            crystal = crystalSelector(it.crystal),
            deuterium = deuteriumSelector(it.deuterium)
          )
      )
  }
}

object Selector {
  def skip: Int => Int = _ => 0

  def all: Int => Int = countOnPlanet => countOnPlanet

  def decreaseBy(value: Int): Int => Int = countOnPlanet => Math.max(countOnPlanet - value, 0)
}
