package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetAttitude.Hostile
import not.ogame.bots.FleetMissionType.Spy
import not.ogame.bots.ordon.utils.Noise
import not.ogame.bots.{Fleet, LocalClock, OgameDriver}

class AlertOgameAction[T[_]: Monad](implicit clock: LocalClock) extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      allFleets <- ogame.readAllFleets()
      _ = alert(allFleets)
    } yield clock.now().plusMinutes(3)

  def alert(allFleets: List[Fleet]): Unit = {
    val maybeHostileFleet = allFleets.find(fleet => fleet.fleetAttitude == Hostile && fleet.fleetMissionType != Spy)
    if (maybeHostileFleet.isDefined) {
      Noise.makeNoise()
    }
  }
}
