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
    maxNumberOfExpeditions: Int,
    startPlanet: PlayerPlanet,
    targetList: List[Coordinates]
)(implicit clock: LocalClock)
    extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    ogame
      .readAllFleets()
      .flatMap(processFleet(ogame, _))
  }

  def processFleet(ogame: OgameDriver[T], fleets: List[Fleet]): T[ZonedDateTime] = {
    if (fleets.count(returningExpedition) >= maxNumberOfExpeditions) {
      fleets.filter(_.fleetMissionType == Expedition).map(_.arrivalTime).min.pure[T]
    } else {
      sendFleet(ogame)
    }
  }

  def returningExpedition(fleet: Fleet): Boolean = {
    fleet.fleetMissionType == Expedition && fleet.isReturning
  }

  def sendFleet(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      myFleets <- ogame.readMyFleets()
      fleetOnPlanet <- ogame.readFleetPage(startPlanet.id)
      returningExpeditionFleets = myFleets.fleets.filter(f => f.isReturning && f.fleetMissionType == Expedition)
      flyingSmallCargoCount = returningExpeditionFleets.map(_.ships(SmallCargoShip)).sum
      flyingLargeCargoCount = returningExpeditionFleets.map(_.ships(LargeCargoShip)).sum
      flyingExplorerCount = returningExpeditionFleets.map(_.ships(Explorer)).sum
      smallCargoToSend = (fleetOnPlanet.ships(SmallCargoShip) + flyingSmallCargoCount) / maxNumberOfExpeditions + 1
      largeCargoToSend = (fleetOnPlanet.ships(LargeCargoShip) + flyingLargeCargoCount) / maxNumberOfExpeditions + 1
      explorerToSend = (fleetOnPlanet.ships(Explorer) + flyingExplorerCount) / maxNumberOfExpeditions
      topBattleShip = getTopBattleShip(fleetOnPlanet)
      targetCoordinates = Random.shuffle(targetList).head
      _ <- new SendFleet(
        from = startPlanet,
        to = PlayerPlanet(PlanetId.apply(""), targetCoordinates),
        selectResources = _ => Resources.Zero,
        selectShips = _ =>
          Map(
            SmallCargoShip -> smallCargoToSend,
            LargeCargoShip -> largeCargoToSend,
            Explorer -> explorerToSend,
            EspionageProbe -> 1,
            topBattleShip -> 1
          ),
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
