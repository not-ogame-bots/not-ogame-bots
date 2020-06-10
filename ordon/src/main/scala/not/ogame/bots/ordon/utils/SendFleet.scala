package not.ogame.bots.ordon.utils

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots._

class SendFleet(
    from: PlayerPlanet,
    to: PlayerPlanet,
    selectResources: SelectResources = fleetPageData => fleetPageData.currentResources,
    selectShips: SelectShips = fleetPageData => fleetPageData.ships,
    fleetSpeed: FleetSpeed = FleetSpeed.Percent100
) {
  def sendDeployment[T[_]: Monad](ogameDriver: OgameDriver[T]): T[ZonedDateTime] =
    for {
      fleetPage <- ogameDriver.readFleetPage(from.id)
      _ <- ogameDriver.sendFleet(
        SendFleetRequest(
          from = from,
          ships = SendFleetRequestShips.Ships(selectShips(fleetPage)),
          targetCoordinates = to.coordinates,
          fleetMissionType = Deployment,
          resources = FleetResources.Given(selectResources(fleetPage)),
          speed = fleetSpeed
        )
      )
      fleets <- ogameDriver.readAllFleets()
      thisFleet = fleets
        .find(fleet => fleet.fleetMissionType == Deployment && fleet.from == from.coordinates && fleet.to == to.coordinates)
        .get
      // There is an issue in ogame that fleet just after arrival is still on fleet list as returning. To avoid that 3 second delay was added.
    } yield thisFleet.arrivalTime.plusSeconds(3)
}

trait SelectResources extends (FleetPageData => Resources)

trait SelectShips extends (FleetPageData => Map[ShipType, Int])

class FleetSelector(filters: Map[ShipType, Int => Int] = Map()) extends SelectShips {
  override def apply(fleetPageData: FleetPageData): Map[ShipType, Int] = {
    ShipType.values.map(shipType => shipType -> computeShipToSend(shipType, fleetPageData.ships(shipType))).toMap.filter {
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

class ResourceSelector(
    metalSelector: Int => Int = Selector.all,
    crystalSelector: Int => Int = Selector.all,
    deuteriumSelector: Int => Int = Selector.all
) extends SelectResources {
  override def apply(fleetPageData: FleetPageData): Resources = {
    Resources(
      metal = metalSelector(fleetPageData.currentResources.metal),
      crystal = crystalSelector(fleetPageData.currentResources.crystal),
      deuterium = deuteriumSelector(fleetPageData.currentResources.deuterium)
    )
  }
}

object Selector {
  def skip: Int => Int = _ => 0

  def all: Int => Int = countOnPlanet => countOnPlanet

  def decreaseBy(value: Int): Int => Int = countOnPlanet => Math.max(countOnPlanet - value, 0)

  def atMost(value: Int): Int => Int = countOnPlanet => Math.min(countOnPlanet, value)
}
