package not.ogame.bots.ordon

import java.awt.Toolkit
import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetAttitude.Hostile
import not.ogame.bots.{Fleet, LocalClock, OgameDriver}

class AlertOgameAction[T[_]: Monad](implicit clock: LocalClock) extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      allFleets <- ogame.readAllFleets()
      _ = alert(allFleets)
    } yield clock.now().plusMinutes(3)

  def alert(allFleets: List[Fleet]): Unit = {
    val maybeHostileFleet = allFleets.find(_.fleetAttitude == Hostile)
    if (maybeHostileFleet.isDefined) {
      for (a <- 1 to 30) {
        Toolkit.getDefaultToolkit.beep()
        Thread.sleep(100)
      }
    }
  }
}
