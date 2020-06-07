package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots.ShipType._
import not.ogame.bots._

class FsCargoOgameAction[T[_]: Monad](
    expeditionMoon: PlayerPlanet,
    otherMoon: PlayerPlanet,
    fsPlanet: PlayerPlanet
)(implicit clock: LocalClock)
    extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    ogame.readAllFleets().flatMap(fleets => handleFleetList(ogame, fleets))
  }

  private def handleFleetList(ogame: OgameDriver[T], fleets: List[Fleet]): T[ZonedDateTime] = {
    val thisFleetList = fleets.filter(isThisFleet)
    if (thisFleetList.nonEmpty) {
      // There is an issue in ogame that fleet just after arrival is still on fleet list as returning. To avoid that 3 second delay was added.
      thisFleetList.head.arrivalTime.plusSeconds(3).pure[T]
    } else {
      ogame.checkFleetOnPlanet(otherMoon.id).flatMap(shipsCountOnOtherMoon => handleShipsCount(ogame, shipsCountOnOtherMoon))
    }
  }

  private def isThisFleet(fleet: Fleet): Boolean = {
    val fromExpeditionMoon = fleet.from == expeditionMoon.coordinates && fleet.to == otherMoon.coordinates
    val fromOtherMoon = fleet.from == otherMoon.coordinates && fleet.to == expeditionMoon.coordinates
    (fromExpeditionMoon || fromOtherMoon) && fleet.fleetMissionType == Deployment
  }

  private def handleShipsCount(ogame: OgameDriver[T], shipsCountOnOtherMoon: Map[ShipType, Int]): T[ZonedDateTime] = {
    if (shipsCountOnOtherMoon.values.sum > 0) {
      sendFleetFromOtherMoon(ogame)
    } else {
      sendFleetFromExpeditionMoon(ogame)
    }
  }

  private def sendFleetFromOtherMoon(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    ogame
      .sendFleet(
        SendFleetRequest(
          from = otherMoon,
          ships = SendFleetRequestShips.AllShips,
          targetCoordinates = expeditionMoon.coordinates,
          fleetMissionType = Deployment,
          resources = FleetResources.Max,
          speed = FleetSpeed.Percent30
        )
      )
      .map(_ => clock.now())
  }

  private def sendFleetFromExpeditionMoon(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    ogame.readSuppliesPage(expeditionMoon.id).flatMap(page => handleResources(ogame, page))
  }

  def handleResources(ogame: OgameDriver[T], suppliesPageData: SuppliesPageData): T[ZonedDateTime] = {
    val total = suppliesPageData.currentResources.metal + suppliesPageData.currentResources.crystal + suppliesPageData.currentResources.deuterium
    if (total > 10_000 * 7_000) {
      sendFleetToFsPlanet(ogame)
    } else {
      sendFleetToOtherMoon(ogame)
    }
  }

  private def sendFleetToFsPlanet(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      resources <- new ResourceSelector[T](deuteriumSelector = Selector.decreaseBy(500_000)).selectResources(ogame, expeditionMoon)
      smallCargoCount = (resources.metal + resources.crystal + resources.deuterium) / 7_000
      fleet <- new FleetSelector[T](
        filters = Map(
          Destroyer -> Selector.decreaseBy(6),
          EspionageProbe -> Selector.decreaseBy(50),
          Explorer -> Selector.skip,
          LargeCargoShip -> Selector.skip,
          SmallCargoShip -> Selector.atMost(smallCargoCount + 1)
        )
      ).selectShips(ogame, expeditionMoon)
      _ <- ogame.sendFleet(
        SendFleetRequest(
          from = expeditionMoon,
          ships = SendFleetRequestShips.Ships(fleet),
          targetCoordinates = fsPlanet.coordinates,
          fleetMissionType = Deployment,
          resources = FleetResources.Given(resources),
          speed = FleetSpeed.Percent100
        )
      )
    } yield clock.now()

  private def sendFleetToOtherMoon(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      resources <- new ResourceSelector[T](deuteriumSelector = Selector.decreaseBy(300_000)).selectResources(ogame, expeditionMoon)
      fleet <- new FleetSelector[T](
        filters = Map(
          Destroyer -> Selector.decreaseBy(6),
          EspionageProbe -> Selector.decreaseBy(50),
          Explorer -> Selector.skip,
          LargeCargoShip -> Selector.decreaseBy(410)
        )
      ).selectShips(ogame, expeditionMoon)
      _ <- ogame.sendFleet(
        SendFleetRequest(
          from = expeditionMoon,
          ships = SendFleetRequestShips.Ships(fleet),
          targetCoordinates = otherMoon.coordinates,
          fleetMissionType = Deployment,
          resources = FleetResources.Given(resources),
          speed = FleetSpeed.Percent30
        )
      )
    } yield clock.now()
}
