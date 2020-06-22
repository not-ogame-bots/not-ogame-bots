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
      sendFleet(ogame, page.fleetSlots.maxExpeditions)
    }
  }

  private def sendFleet(ogame: OgameDriver[T], maxNumberOfExpeditions: Int): T[ZonedDateTime] =
    for {
      myFleets <- ogame.readMyFleets()
      fleetOnPlanet <- ogame.readFleetPage(startPlanet.id)
      returningExpeditionFleets = myFleets.fleets.filter(f => f.fleetMissionType == Expedition)
      flyingSmallCargoCount = returningExpeditionFleets.map(_.ships(SmallCargoShip)).sum
      flyingLargeCargoCount = returningExpeditionFleets.map(_.ships(LargeCargoShip)).sum
      flyingExplorerCount = returningExpeditionFleets.map(_.ships(Explorer)).sum
      smallCargoToSend = (fleetOnPlanet.ships(SmallCargoShip) + flyingSmallCargoCount - 40) / maxNumberOfExpeditions + 1
      largeCargoToSend = (fleetOnPlanet.ships(LargeCargoShip) + flyingLargeCargoCount) / maxNumberOfExpeditions
      explorerToSend = (fleetOnPlanet.ships(Explorer) + flyingExplorerCount - 7) / maxNumberOfExpeditions
      topBattleShip = getTopBattleShip(fleetOnPlanet)
      shipsToSend = Map(
        SmallCargoShip -> smallCargoToSend,
        LargeCargoShip -> largeCargoToSend,
        Explorer -> explorerToSend,
        EspionageProbe -> 1,
        topBattleShip -> 1
      )
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
