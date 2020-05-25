package not.ogame.bots.ordon

import java.time.Instant

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Expedition
import not.ogame.bots.SendFleetRequestShips.Ships
import not.ogame.bots._

class ExpeditionOgameAction[T[_]: Monad](
    maxNumberOfExpeditions: Int,
    val startPlanetId: String,
    val expeditionFleet: Map[ShipType, Int],
    val targetCoordinates: Coordinates
) extends OgameAction[T] {
  override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] = {
    ogame
      .readAllFleets()
      .flatMap(processFleet(ogame, _))
      .map(resumeOn => List(ScheduledAction(resumeOn, this)))
  }

  def processFleet(ogame: OgameDriver[T], fleets: List[Fleet]): T[Instant] = {
    if (fleets.count(returningExpedition) >= maxNumberOfExpeditions) {
      fleets.filter(_.fleetMissionType == Expedition).map(_.arrivalTime).min.pure[T]
    } else {
      sendFleet(ogame)
    }
  }

  def returningExpedition(fleet: Fleet): Boolean = {
    fleet.fleetMissionType == Expedition && fleet.isReturning
  }

  def sendFleet(ogame: OgameDriver[T]): T[Instant] = {
    ogame
      .sendFleet(
        SendFleetRequest(
          ???,
          Ships(expeditionFleet),
          targetCoordinates,
          Expedition,
          FleetResources.Given(Resources.Zero)
        )
      )
      .map(_ => Instant.now())
  }
}
