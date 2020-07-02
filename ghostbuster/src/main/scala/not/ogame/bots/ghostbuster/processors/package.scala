package not.ogame.bots.ghostbuster

import java.time.ZonedDateTime

import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import retry.RetryPolicies
import retry.syntax.all._
import cats.implicits._
import not.ogame.bots.PlayerPlanet

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

package object processors extends StrictLogging {
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

  def max(first: ZonedDateTime, second: ZonedDateTime): ZonedDateTime = {
    if (first.isBefore(second)) {
      second
    } else {
      first
    }
  }

  def withRetry[T](task: Task[T])(flowName: String): Task[T] = {
    val policy = RetryPolicies.capDelay[Task](5 minutes, RetryPolicies.exponentialBackoff[Task](2 seconds))
    task.retryingOnAllErrors(
      policy,
      onError = { (e, details) =>
        logger.error(s"Restarting: $flowName. Retry details :$details", e).pure[Task]
      }
    )
  }

  def showCoordinates(planet: PlayerPlanet): String = {
    s"${planet.coordinates.galaxy}:${planet.coordinates.system}:${planet.coordinates.position}"
  }
}
