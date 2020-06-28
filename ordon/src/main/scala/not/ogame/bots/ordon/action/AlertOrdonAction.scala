package not.ogame.bots.ordon.action

import java.time.ZonedDateTime

import cats.implicits._
import not.ogame.bots.FleetAttitude.Hostile
import not.ogame.bots.FleetMissionType.Spy
import not.ogame.bots.ordon.core.{OrdonOgameDriver, TimeBasedOrdonAction}
import not.ogame.bots.ordon.utils.Noise

class AlertOrdonAction extends TimeBasedOrdonAction {
  override def processTimeBased(ogame: OrdonOgameDriver): ZonedDateTime = {
    if (ogame.readAllFleets().exists(fleet => fleet.fleetAttitude == Hostile && fleet.fleetMissionType != Spy)) {
      Noise.makeNoise()
    }
    ZonedDateTime.now().plusMinutes(3)
  }
}
