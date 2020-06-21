package not.ogame.bots.ordon

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Attack
import not.ogame.bots._
import not.ogame.bots.ordon.utils.SendFleet

class AttackOgameAction[T[_]: Monad](from: PlayerPlanet)(implicit clock: LocalClock) extends OgameAction[T] {
  private val sendFleet = new SendFleet(
    from = from,
    to = PlayerPlanet(PlanetId.apply(""), Coordinates(1, 155, 6)),
    selectResources = _ => Resources.Zero,
    missionType = Attack
  )

  override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] =
    for {
      page <- ogame.readMyFleets()
      resumeOn <- if (clock.now().getHour >= 5) clock.now().plusDays(1).pure[T]
      else if (page.fleets.isEmpty) sendFleet.sendFleet(ogame)
      else page.fleets.head.arrivalTime.plusSeconds(3).pure[T]
    } yield List(ScheduledAction(resumeOn, this))
}
