package not.ogame.bots.ghostbuster

import java.time.ZonedDateTime

import scala.concurrent.duration.FiniteDuration

package object processors {
  implicit class RichZonedDateTime(zdt: ZonedDateTime) {
    def plus(fd: FiniteDuration): ZonedDateTime = {
      zdt.plusSeconds(fd.toSeconds)
    }

    def minus(fd: FiniteDuration): ZonedDateTime = {
      zdt.minusSeconds(fd.toSeconds)
    }
  }
}
