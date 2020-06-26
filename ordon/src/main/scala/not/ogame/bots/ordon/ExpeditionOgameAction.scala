package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Expedition
import not.ogame.bots._
import not.ogame.bots.ordon.utils.SendFleet

import scala.util.Random

class ExpeditionOgameAction[T[_]: Monad](
    startPlanet: PlayerPlanet,
    expeditionFleet: Map[ShipType, Int],
    targetList: List[Coordinates]
)(implicit clock: LocalClock)
    extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    ogame
      .readMyFleets()
      .flatMap(processFleet(ogame, _))
  }

  def processFleet(ogame: OgameDriver[T], page: MyFleetPageData): T[ZonedDateTime] = {
    if (page.fleetSlots.currentExpeditions >= page.fleetSlots.maxExpeditions) {
      page.fleets.filter(_.fleetMissionType == Expedition).map(_.arrivalTime).min.pure[T]
    } else {
      sendFleet(ogame)
    }
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
