package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Expedition
import not.ogame.bots.SendFleetRequestShips.Ships
import not.ogame.bots._

class ExpeditionOgameAction[T[_]: Monad](
    maxNumberOfExpeditions: Int,
    startPlanet: PlayerPlanet,
    expeditionFleet: Map[ShipType, Int],
    targetCoordinates: Coordinates
)(implicit clock: LocalClock)
    extends OgameAction[T] {
  override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] = {
    ogame
      .readAllFleets()
      .flatMap(processFleet(ogame, _))
      .map(resumeOn => List(ScheduledAction(resumeOn, this)))
  }

  def processFleet(ogame: OgameDriver[T], fleets: List[Fleet]): T[ZonedDateTime] = {
    if (fleets.count(returningExpedition) >= maxNumberOfExpeditions) {
      fleets.filter(_.fleetMissionType == Expedition).map(_.arrivalTime.toZdt).min.pure[T]
    } else {
      sendFleet(ogame)
    }
  }

  def returningExpedition(fleet: Fleet): Boolean = {
    fleet.fleetMissionType == Expedition && fleet.isReturning
  }

  def sendFleet(ogame: OgameDriver[T]): T[ZonedDateTime] = {
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
