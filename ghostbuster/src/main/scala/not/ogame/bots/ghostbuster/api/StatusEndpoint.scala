package not.ogame.bots.ghostbuster.api

import cats.effect.concurrent.Ref
import cats.implicits._
import io.circe.refined._
import monix.eval.Task
import not.ogame.bots.ghostbuster.reporting.State
import sttp.tapir.server.ServerEndpoint

class StatusEndpoint(state: Ref[Task, State]) extends HttpCommons {
  val getStatus: ServerEndpoint[Unit, Unit, State, Nothing, Task] = endpoint.get
    .in("status")
    .out(jsonBody[State])
    .serverLogic { _ =>
      state.get.map(_.asRight[Unit])
    }
}
