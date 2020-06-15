package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.reactive.Consumer
import not.ogame.bots._
import not.ogame.bots.ghostbuster.executor.Notification
import not.ogame.bots.ghostbuster.{EscapeConfig, FLogger}

class EscapeFleetProcessor(taskExecutor: TaskExecutor, escapeConfig: EscapeConfig)(implicit clock: LocalClock) extends FLogger {
  def run(): Task[Unit] = {
    (for {
      planets <- taskExecutor.readPlanetsAndMoons()
      fleets <- taskExecutor.readAllFleets()
      _ <- loop(fleets, planets)
    } yield ())
      .onErrorRestartIf(_ => true)
  }

  private def loop(fleets: List[Fleet], planets: List[PlayerPlanet]): Task[Unit] = {
    processFleets(planets, fleets)
      .flatMap(
        dateTime =>
          Task
            .race(
              taskExecutor.waitTo(dateTime) >> Logger[Task].info("Reading fleets normally"),
              taskExecutor.subscribeToNotifications
                .collect { case n: Notification.ReadAllFleets => n.value }
                .consumeWith(Consumer.head) <* Logger[Task].info("Used fleets from notification")
            )
            .flatMap {
              case Left(_)      => taskExecutor.readAllFleets()
              case Right(value) => Task.pure(value)
            }
      )
      .flatMap(newFleets => loop(newFleets, planets))
  }

  private def processFleets(planets: List[PlayerPlanet], fleets: List[Fleet]) = {
    val hostileFleets = fleets.filter(f => f.fleetMissionType == FleetMissionType.Attack && f.fleetAttitude == FleetAttitude.Hostile)
    if (hostileFleets.nonEmpty) {
      val firstFleet = hostileFleets.minBy(_.arrivalTime)
      val remainingTime = timeDiff(clock.now(), firstFleet.arrivalTime)
      if (remainingTime < escapeConfig.minEscapeTime) {
        Logger[Task].info("Hostile fleet is too close, we are doomed!").map(_ => clock.now().plus(escapeConfig.interval))
      } else if (remainingTime < escapeConfig.escapeTimeThreshold) {
        checkAndEscape(firstFleet, planets).map(_ => clock.now())
      } else {
        val nextInterval = clock.now().plus(escapeConfig.interval)
        val minTimeToWait = min(firstFleet.arrivalTime.minus(escapeConfig.escapeTimeThreshold), nextInterval)
        Logger[Task]
          .info(s"Hostile fleet detected but is quite far (${remainingTime.toSeconds} seconds). Waiting... $minTimeToWait")
          .map(_ => minTimeToWait)
      }
    } else {
      Logger[Task].info("No hostile fleets detected. Sleeping...").map(_ => clock.now().plus(escapeConfig.interval))
    }
  }

  private def checkAndEscape(firstFleet: Fleet, planets: List[PlayerPlanet]): Task[Unit] = {
    for {
      _ <- Logger[Task].info(s"Hostile fleet is close enough, checking fleet on planet ${firstFleet.to}!")
      planetUnderAttack = planets.find(_.coordinates == firstFleet.to).get
      ourFleet <- taskExecutor.getFleetOnPlanet(planetUnderAttack)
      _ <- if (ourFleet.fleet.values.sum > 0) {
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
      _ <- taskExecutor.sendFleet(
        SendFleetRequest(
          planetUnderAttack,
          SendFleetRequestShips.AllShips,
          escapeConfig.target,
          missionType,
          FleetResources.Max,
          FleetSpeed.Percent10
        )
      )
      _ <- Logger[Task].info(s"Waiting to attack time: $attackTime")
      _ <- taskExecutor.waitTo(attackTime)
      _ <- Logger[Task].info("Looking for escaped fleet...")
      myFleets <- taskExecutor.readMyFleets()
      escapedFleet = myFleets.fleets.find(
        f => f.from == planetUnderAttack.coordinates && f.to == escapeConfig.target && f.fleetMissionType == missionType
      )
      _ <- escapedFleet match {
        case Some(value) => Logger[Task].info("Returning fleet...") >> taskExecutor.returnFleet(value.fleetId)
        case None        => Logger[Task].warn("Couldn't find our escaped fleet :(")
      }
    } yield ()
  }
}
