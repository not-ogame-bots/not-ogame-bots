package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Expedition
import not.ogame.bots._
import not.ogame.bots.ordon.utils.SendFleet

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
    new SendFleet(
      from = startPlanet,
      to = PlayerPlanet(PlanetId.apply(""), targetCoordinates),
      selectResources = _ => Resources.Zero,
      selectShips = page => page.ships.map(e => e._1 -> Math.min(e._2, expeditionFleet.getOrElse(e._1, 0))),
      missionType = Expedition
    ).sendFleet(ogame).map(_ => clock.now())
  }
}
