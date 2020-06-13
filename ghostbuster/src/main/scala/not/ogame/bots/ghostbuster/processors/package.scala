package not.ogame.bots.ghostbuster

import java.time.ZonedDateTime

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

package object processors {
  implicit class RichZonedDateTime(zdt: ZonedDateTime) {
    def plus(fd: FiniteDuration): ZonedDateTime = {
      zdt.plusSeconds(fd.toSeconds)
    }

    def minus(fd: FiniteDuration): ZonedDateTime = {
      zdt.minusSeconds(fd.toSeconds)
    }
  }

  def timeDiff(earlier: ZonedDateTime, later: ZonedDateTime): FiniteDuration = {
    java.time.Duration.between(earlier, later).toScala
  }

  def min(first: ZonedDateTime, second: ZonedDateTime): ZonedDateTime = {
    if (first.isBefore(second)) {
      first
    } else {
      second
    }
  }
}
