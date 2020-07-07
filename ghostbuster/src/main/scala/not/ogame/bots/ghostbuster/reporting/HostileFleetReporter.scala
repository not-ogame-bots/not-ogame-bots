package not.ogame.bots.ghostbuster.reporting

import cats.Eq
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.reactive.Consumer
import not.ogame.bots.ghostbuster.FLogger
import not.ogame.bots.ghostbuster.executor.OgameActionExecutor
import not.ogame.bots.ghostbuster.infrastructure.{Channel, SlackService}
import not.ogame.bots.ghostbuster.notifications.{Notification, Notifier}
import not.ogame.bots.{Fleet, FleetAttitude, FleetMissionType, LocalClock}

class HostileFleetReporter(slackService: SlackService[Task], notifier: Notifier)(implicit clock: LocalClock) extends FLogger {
  private implicit val uEq: Eq[Fleet] = Eq.fromUniversalEquals

  def run(): Task[Unit] = {
    notifier.subscribeToNotifications
      .collect {
        case Notification.ReadAllFleets(fleets) =>
          fleets.filter(
            f =>
              f.fleetAttitude == FleetAttitude.Hostile && (f.fleetMissionType == FleetMissionType.Destroy || f.fleetMissionType == FleetMissionType.Attack)
          )
      }
      .distinctUntilChanged
      .concatMapIterable(identity)
      .consumeWith(Consumer.foreachTask { fleet =>
        Logger[Task].warn(s"!!!! HOSTILE FLEET DETECTED ${pprint.apply(fleet)} !!!!") >>
          slackService
            .postMessage(
              s"@Channel My Imperator, a hostile fleet has been detected. It will arrive at ${fleet.arrivalTime} on ${fleet.to}",
              Channel.Alerts
            )
      })
  }
}
