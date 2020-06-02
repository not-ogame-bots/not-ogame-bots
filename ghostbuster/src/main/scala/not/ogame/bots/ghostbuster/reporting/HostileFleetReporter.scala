package not.ogame.bots.ghostbuster.reporting

import cats.Eq
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.reactive.Consumer
import not.ogame.bots.ghostbuster.FLogger
import not.ogame.bots.ghostbuster.executor.Notification
import not.ogame.bots.ghostbuster.infrastructure.{FCMService, PushNotificationRequest}
import not.ogame.bots.ghostbuster.processors.TaskExecutor
import not.ogame.bots.{Fleet, FleetAttitude}

class HostileFleetReporter(fcmService: FCMService[Task], taskExecutor: TaskExecutor) extends FLogger {
  private implicit val uEq: Eq[Fleet] = Eq.fromUniversalEquals

  def run(): Task[Unit] = {
    taskExecutor.subscribeToNotifications
      .collect { case Notification.GetAirFleet(fleets) => fleets.filter(_.fleetAttitude == FleetAttitude.Hostile) }
      .distinctUntilChanged
      .flatMapIterable(identity)
      .consumeWith(Consumer.foreachTask { fleet =>
        Logger[Task].warn(s"!!!! HOSTILE FLEET DETECTED ${pprint.apply(fleet)} !!!!") >>
          fcmService.sendMessageWithoutData(
            PushNotificationRequest(
              "Attention",
              s"My Imperator, a hostile fleet has been detected. It will arrive at ${fleet.arrivalTime}",
              "attacks",
              null
            )
          )
      })
  }
}
