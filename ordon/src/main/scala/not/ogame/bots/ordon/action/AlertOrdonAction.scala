package not.ogame.bots.ordon.action

import java.time.ZonedDateTime

import not.ogame.bots.FleetAttitude.Hostile
import not.ogame.bots.FleetMissionType.Spy
import not.ogame.bots.ordon.core.{EventRegistry, OrdonOgameDriver, TimeBasedOrdonAction}
import not.ogame.bots.ordon.utils.Noise

class AlertOrdonAction extends TimeBasedOrdonAction {
  override def processTimeBased(ogame: OrdonOgameDriver, eventRegistry: EventRegistry): ZonedDateTime = {
    if (ogame.readAllFleets().exists(fleet => fleet.fleetAttitude == Hostile && fleet.fleetMissionType != Spy)) {
      Noise.makeNoise()
    }
    ZonedDateTime.now().plusMinutes(3)
  }

  override def toString: String = s"Alert $resumeOn"
}
