package not.ogame.bots.ghostbuster.executor
import java.time.ZonedDateTime

import monix.reactive.Observable
import not.ogame.bots.ghostbuster.notifications.Notification
import not.ogame.bots.ghostbuster.ogame.OgameAction

trait OgameActionExecutor[F[_]] {
  def execute[A](action: OgameAction[A]): F[A]

  def waitTo(time: ZonedDateTime): F[Unit]
}
