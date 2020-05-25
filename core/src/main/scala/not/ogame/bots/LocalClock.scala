package not.ogame.bots

import java.time.{Clock, ZoneOffset, ZonedDateTime}

trait LocalClock {
  def now(): ZonedDateTime
}

class RealLocalClock(zoneOffset: ZoneOffset = ZoneOffset.ofHours(2)) extends LocalClock {
  private val clock = Clock.system(zoneOffset)

  override def now(): ZonedDateTime = {
    ZonedDateTime.now(clock)
  }
}
