package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Colonization
import not.ogame.bots.ShipType.ColonyShip
import not.ogame.bots._
import not.ogame.bots.ordon.utils.SendFleet

class ColoniseAction[T[_]: Monad](implicit clock: LocalClock) extends OgameAction[T] {
  private val planet2 = PlayerPlanet(PlanetId.apply("33623552"), Coordinates(1, 155, 7))

  override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] = {
    val now = ZonedDateTime.now()
    if (now.getHour == 3 && now.getMinute == 0) {
      new SendFleet(
        from = planet2,
        to = PlayerPlanet(PlanetId.apply(""), Coordinates(1, 155, 6)),
        selectResources = _ => Resources.Zero,
        selectShips = _ => Map(ColonyShip -> 1),
        missionType = Colonization
      ).sendFleet(ogame).map(_ => List[ScheduledAction[T]]())
    } else {
      List[ScheduledAction[T]]().pure[T]
    }
  }
}
