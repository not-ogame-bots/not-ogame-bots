package not.ogame.bots.ordon

import java.time.LocalDateTime

import cats.Monad
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
    Monad[T].map(Monad[T].flatMap(ogame.readAllFleets())(processFleet(ogame, _)))(resumeOn => List(ScheduledAction(resumeOn, this)))
  }

  def processFleet(ogame: OgameDriver[T], fleets: List[Fleet]): T[LocalDateTime] = {
    if (fleets.count(returningExpedition) >= maxNumberOfExpeditions) {
      Monad[T].pure(fleets.filter(_.fleetMissionType == Expedition).map(_.arrivalTime).min)
    } else {
      sendFleet(ogame)
    }
  }

  def returningExpedition(fleet: Fleet): Boolean = {
    fleet.fleetMissionType == Expedition && fleet.isReturning
  }

  def sendFleet(ogame: OgameDriver[T]): T[LocalDateTime] = {
    Monad[T].map(
      ogame.sendFleet(
        SendFleetRequest(
          startPlanetId,
          Ships(expeditionFleet),
          targetCoordinates,
          Expedition,
          Resources(0, 0, 0)
        )
      )
    )(_ => LocalDateTime.now())
  }
}
