package not.ogame.bots.ghostbuster.api

import monix.eval.Task
import not.ogame.bots.ghostbuster.actions.SpreadResourcesAction
import sttp.tapir.server.ServerEndpoint
import cats.implicits._

class SpreadResourcesEndpoint(spreadResourcesAction: SpreadResourcesAction) extends HttpCommons {
  val spreadEndpoint: ServerEndpoint[SpreadResourcesAction.Request, String, Unit, Nothing, Task] = endpoint.post
    .in("spread")
    .in(jsonBody[SpreadResourcesAction.Request])
    .errorOut(stringBody)
    .serverLogic { req =>
      spreadResourcesAction
        .run(req)
        .map(_.asRight[String])
        .handleError(e => e.getMessage.asLeft[Unit])
    }
}
