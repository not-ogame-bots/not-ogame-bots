package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Expedition
import not.ogame.bots.SendFleetRequestShips.Ships
import not.ogame.bots._

import scala.util.Random

class ExpeditionOgameAction[T[_]: Monad](
    maxNumberOfExpeditions: Int,
    startPlanet: PlayerPlanet,
    expeditionFleet: Map[ShipType, Int],
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

  def sendFleet(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    val targetCoordinates = Random.shuffle(targetList).head
    ogame
      .sendFleet(
        SendFleetRequest(
          startPlanet,
          Ships(expeditionFleet),
          targetCoordinates,
          Expedition,
          FleetResources.Given(Resources.Zero)
        )
      )
      .map(_ => clock.now())
  }
}
