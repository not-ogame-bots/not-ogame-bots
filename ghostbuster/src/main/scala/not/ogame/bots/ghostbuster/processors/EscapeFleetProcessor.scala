package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.reactive.Consumer
import not.ogame.bots._
import not.ogame.bots.ghostbuster.executor._
import not.ogame.bots.ghostbuster.notifications.Notification
import not.ogame.bots.ghostbuster.ogame.OgameAction
import not.ogame.bots.ghostbuster.FLogger

import scala.concurrent.duration.FiniteDuration

class EscapeFleetProcessor(ogameDriver: OgameDriver[OgameAction], escapeConfig: EscapeConfig)(
    implicit executor: OgameActionExecutor[Task],
    clock: LocalClock
) extends FLogger {
  def run(): Task[Unit] = {
    for {
      planets <- ogameDriver.readPlanets().execute()
      fleets <- ogameDriver.readAllFleetsRedirect().execute()
      _ <- loop(fleets, planets)
    } yield ()
  }

  private def loop(fleets: List[Fleet], planets: List[PlayerPlanet]): Task[Unit] = {
    processFleets(planets, fleets)
      .flatMap(
        dateTime =>
          Task
            .race(
              executor.waitTo(dateTime) >> Logger[Task].info("Reading fleets normally"),
              executor.subscribeToNotifications
                .collect { case n: Notification.ReadAllFleets => n.fleets }
                .consumeWith(Consumer.head) <* Logger[Task].info("Used fleets from notification")
            )
            .flatMap {
              case Left(_)      => ogameDriver.readAllFleetsRedirect().execute()
              case Right(value) => Task.pure(value)
            }
      )
      .flatMap(newFleets => withRetry(loop(newFleets, planets))("escape"))
  }

  private def processFleets(planets: List[PlayerPlanet], fleets: List[Fleet]) = {
    val hostileFleets = fleets.filter(f => f.fleetMissionType == FleetMissionType.Attack && f.fleetAttitude == FleetAttitude.Hostile)
    if (hostileFleets.nonEmpty) {
      val firstFleet = hostileFleets.minBy(_.arrivalTime)
      val remainingTime = timeDiff(clock.now(), firstFleet.arrivalTime)
      if (remainingTime < escapeConfig.minEscapeTime) {
        Logger[Task].info("Hostile fleet is too close, we are doomed!").as(clock.now().plus(escapeConfig.interval))
      } else if (remainingTime < escapeConfig.escapeTimeThreshold) {
        checkAndEscape(firstFleet, planets).as(clock.now())
      } else {
        val nextInterval = clock.now().plus(escapeConfig.interval)
        val minTimeToWait = min(firstFleet.arrivalTime.minus(escapeConfig.escapeTimeThreshold), nextInterval)
        Logger[Task]
          .info(s"Hostile fleet detected but is quite far (${remainingTime.toSeconds} seconds). Waiting... $minTimeToWait")
          .as(minTimeToWait)
      }
    } else {
      Logger[Task].info("No hostile fleets detected. Sleeping...").as(clock.now().plus(escapeConfig.interval))
    }
  }

  private def checkAndEscape(firstFleet: Fleet, planets: List[PlayerPlanet]): Task[Unit] = {
    for {
      _ <- Logger[Task].info(s"Hostile fleet is close enough, checking fleet on planet ${firstFleet.to}!")
      planetUnderAttack = planets.find(_.coordinates == firstFleet.to).get
      ourFleet <- ogameDriver.readFleetPage(planetUnderAttack.id).execute()
      _ <- if (ourFleet.ships.values.sum > 0) {
        escapeFrom(planetUnderAttack, firstFleet.arrivalTime)
      } else {
        Logger[Task].info("There are no ships on given planet. Ignoring attack...")
      }
    } yield ()
  }

  private def escapeFrom(planetUnderAttack: PlayerPlanet, attackTime: ZonedDateTime): Task[Unit] = {
    val missionType = FleetMissionType.Transport
    for {
      _ <- Logger[Task].info("Found some ships on attacking planet. Escaping...")
      myFleet <- ogameDriver
        .sendAndTrackFleet(
          SendFleetRequest(
            planetUnderAttack,
            SendFleetRequestShips.AllShips,
            escapeConfig.target,
            missionType,
            FleetResources.Max,
            FleetSpeed.Percent10
          )
        )
        .execute()
      _ <- Logger[Task].info(s"Waiting to attack time: $attackTime")
      _ <- executor.waitTo(attackTime)
      _ <- Logger[Task].info("Returning fleet...")
      _ <- ogameDriver.returnFleet(myFleet.fleetId).execute()
    } yield ()
  }
}
case class EscapeConfig(target: Coordinates, interval: FiniteDuration, minEscapeTime: FiniteDuration, escapeTimeThreshold: FiniteDuration)
