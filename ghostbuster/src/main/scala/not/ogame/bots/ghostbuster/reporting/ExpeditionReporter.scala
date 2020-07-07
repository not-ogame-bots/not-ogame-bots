package not.ogame.bots.ghostbuster.reporting

import monix.eval.Task
import monix.reactive.Consumer
import not.ogame.bots.ghostbuster.infrastructure.{Channel, SlackService}
import not.ogame.bots.ghostbuster.notifications.{Notification, Notifier}

class ExpeditionReporter(slackService: SlackService[Task], notifier: Notifier) {
  def run(): Task[Unit] = {
    notifier.subscribeToNotifications
      .collect {
        case Notification.ExpeditionPhaseOneCompleted(from, fleetId) => s"phase 1 completed from: $from, id: $fleetId"
        case Notification.ExpeditionPhaseTwoCompleted(from, fleetId) => s"phase 2 completed from: $from, id: $fleetId"
        case Notification.ExpeditionDestroyed(from, fleetId)         => s"expedition destroyed from: $from, id: $fleetId"
        case Notification.ExpeditionReturned(from, fleetId)          => s"expedition returned from: $from, id: $fleetId"
      }
      .consumeWith(Consumer.foreachTask(slackService.postMessage(_, Channel.ExpStatus)))
  }
}
