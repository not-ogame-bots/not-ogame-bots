package not.ogame.bots.ghostbuster.notifications

import monix.eval.Task
import monix.reactive.subjects.ConcurrentSubject
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

class Notifier extends NotificationAware {
  private val notifications = ConcurrentSubject.publish[Notification]

  def notify(n: Notification): Task[Unit] = {
    Task.fromFuture(notifications.onNext(n)).void
  }

  override def subscribeToNotifications: Observable[Notification] = notifications
}

trait NotificationAware {
  def subscribeToNotifications: Observable[Notification]
}

