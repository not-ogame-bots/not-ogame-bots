package not.ogame.bots.ghostbuster.api

import cats.implicits._
import monix.eval.Task
import not.ogame.bots.ghostbuster.actions.CollectResourcesAction
import sttp.tapir.server.ServerEndpoint

class CollectResourcesEndpoint(collectResourcesAction: CollectResourcesAction) extends HttpCommons {
  val collectEndpoint: ServerEndpoint[CollectResourcesAction.Request, String, Unit, Nothing, Task] = endpoint.post
    .in("collect")
    .in(jsonBody[CollectResourcesAction.Request])
    .errorOut(stringBody)
    .serverLogic { req =>
      collectResourcesAction
        .run(req)
        .map(_.asRight[String])
        .handleError(e => e.getMessage.asLeft[Unit])
    }
}
