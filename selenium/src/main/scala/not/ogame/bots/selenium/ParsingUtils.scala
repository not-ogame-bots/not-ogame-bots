package not.ogame.bots.selenium

import java.time.ZonedDateTime

import not.ogame.bots.{Coordinates, LocalClock}

object ParsingUtils {
  /** @param coordinatesText Coordinates test in "[1:1:1]" format. */
  def parseCoordinates(coordinatesText: String): Coordinates = {
    if (coordinatesText.length < 5) {
      //Happens when returning from destroyed moon
      return Coordinates(0, 0, 0)
    }
    val coordinates = coordinatesText.split(":").map(_.filter(_.isDigit).toInt)
    Coordinates(coordinates(0), coordinates(1), coordinates(2))
  }

  /** @param timeDigits Time in "12:30:30" format.
    * @return Instant instance with correct time set. Returned instant is always between now and 24h ahead. */
  def parseTimeInFuture(timeDigits: String)(implicit clock: LocalClock): ZonedDateTime = {
    val timeList = timeDigits.split(":").map {
      _.filter(_.isDigit).toInt
    }
    val now = clock.now()
    val localDateTime = now.withHour(timeList(0)).withMinute(timeList(1)).withSecond(timeList(2))
    if (localDateTime.isBefore(now)) {
      localDateTime.plusDays(1)
    } else {
      localDateTime
    }
  }
}
