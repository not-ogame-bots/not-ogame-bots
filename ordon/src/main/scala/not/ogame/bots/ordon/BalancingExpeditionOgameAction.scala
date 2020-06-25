package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Expedition
import not.ogame.bots.ShipType._
import not.ogame.bots._
import not.ogame.bots.ordon.utils.SendFleet

import scala.util.Random

class BalancingExpeditionOgameAction[T[_]: Monad](
    startPlanet: PlayerPlanet,
    targetList: List[Coordinates]
)(implicit clock: LocalClock)
    extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    ogame
      .readMyFleets()
      .flatMap(processFleets(ogame, _))
  }

  def processFleets(ogame: OgameDriver[T], page: MyFleetPageData): T[ZonedDateTime] = {
    if (page.fleetSlots.currentExpeditions == page.fleetSlots.maxExpeditions) {
      page.fleets.filter(_.fleetMissionType == Expedition).map(_.arrivalTime).min.plusSeconds(3).pure[T]
    } else {
      sendFleet(ogame, page)
    }
  }

  private def sendFleet(ogame: OgameDriver[T], myFleets: MyFleetPageData): T[ZonedDateTime] =
    for {
      fleetOnPlanet <- ogame.readFleetPage(startPlanet.id)
      shipsToSend = selectFleet(myFleets, fleetOnPlanet)
      targetCoordinates = Random.shuffle(targetList).head
      _ <- new SendFleet(
        from = startPlanet,
        to = PlayerPlanet(PlanetId.apply(""), targetCoordinates),
        selectResources = _ => Resources.Zero,
        selectShips = _ => {
          shipsToSend
        },
        missionType = Expedition
      ).sendFleet(ogame)
    } yield clock.now()

  private def selectFleet(myFleets: MyFleetPageData, fleetOnPlanet: FleetPageData): Map[ShipType, Int] = {
    val returningExpeditionFleets = myFleets.fleets.filter(f => f.fleetMissionType == Expedition)
    val flyingSmallCargoCount = returningExpeditionFleets.map(_.ships(SmallCargoShip)).sum
    val flyingLargeCargoCount = returningExpeditionFleets.map(_.ships(LargeCargoShip)).sum
    val flyingExplorerCount = returningExpeditionFleets.map(_.ships(Explorer)).sum
    val maxLargeCargo = 300
    val largeCargoToSend =
      Math.min(maxLargeCargo, (fleetOnPlanet.ships(LargeCargoShip) + flyingLargeCargoCount) / myFleets.fleetSlots.maxExpeditions + 1)
    val maxSmallCargo = (maxLargeCargo - largeCargoToSend) / 5
    val smallCargoToSend =
      Math.min(maxSmallCargo, (fleetOnPlanet.ships(SmallCargoShip) + flyingSmallCargoCount) / myFleets.fleetSlots.maxExpeditions + 1)
    val explorerToSend = (fleetOnPlanet.ships(Explorer) + flyingExplorerCount - 7) / myFleets.fleetSlots.maxExpeditions
    val topBattleShip = getTopBattleShip(fleetOnPlanet)
    Map(
      SmallCargoShip -> smallCargoToSend,
      LargeCargoShip -> largeCargoToSend,
      Explorer -> explorerToSend,
      EspionageProbe -> 1,
      topBattleShip -> 1
    )
  }

  private def getTopBattleShip(fleetOnPlanet: FleetPageData): ShipType = {
    if (fleetOnPlanet.ships(Destroyer) > 0) {
      Destroyer
    } else if (fleetOnPlanet.ships(Battleship) > 0) {
      Battleship
    } else if (fleetOnPlanet.ships(Cruiser) > 0) {
      Cruiser
    } else {
      LightFighter
    }
  }
}
