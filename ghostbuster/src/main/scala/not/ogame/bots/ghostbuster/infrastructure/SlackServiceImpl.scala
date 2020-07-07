package not.ogame.bots.ghostbuster.infrastructure

import java.nio.ByteBuffer

import io.circe.generic.auto._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import sttp.client._
import sttp.client.circe._
import sttp.client.okhttp.WebSocketHandler
import sttp.client.okhttp.monix.OkHttpMonixBackend

class SlackServiceImpl(credentials: SlackCredentials) extends SlackService[Task] {
  private implicit val backend: SttpBackend[Task, Observable[ByteBuffer], WebSocketHandler] = OkHttpMonixBackend().runSyncUnsafe()
  private def urlFromToken(token: String) = s"https://hooks.slack.com/services/$token"

  def postMessage(message: String, channel: Channel): Task[Unit] = {
    val stringUri = channel match {
      case Channel.Alerts    => urlFromToken(credentials.statusAlertToken)
      case Channel.Status    => urlFromToken(credentials.statusToken)
      case Channel.ExpStatus => urlFromToken(credentials.statusExpToken)
    }
    basicRequest
      .post(uri"$stringUri")
      .body(SlackMessage(message))
      .send()
      .map { r =>
        println(r.toString())
      }
      .void
  }
}

case class SlackMessage(text: String)

case class SlackCredentials(statusToken: String, statusAlertToken: String, statusExpToken: String)

trait SlackService[F[_]] {
  def postMessage(message: String, channel: Channel): F[Unit]
}

sealed trait Channel
object Channel {
  case object Alerts extends Channel
  case object Status extends Channel
  case object ExpStatus extends Channel
}
