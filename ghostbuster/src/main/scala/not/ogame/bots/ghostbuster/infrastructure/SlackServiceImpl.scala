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
  private val stringUri = s"https://hooks.slack.com/services/${credentials.statusToken}"

  def postMessage(message: String): Task[Unit] = {
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

case class SlackCredentials(statusToken: String)

trait SlackService[F[_]] {
  def postMessage(message: String): F[Unit]
}
