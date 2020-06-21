package not.ogame.bots.ghostbuster.executor

import cats.effect.concurrent.MVar
import monix.eval.Task

package object impl {
  final case class Request[T](action: Task[T], response: MVar[Task, Response[T]])
  object Request {
    def apply[T](action: Task[T]): Task[Request[T]] = {
      MVar.empty[Task, Response[T]].map(new Request[T](action, _))
    }
  }

  sealed trait Response[T]
  object Response {
    final case class Success[T](value: T) extends Response[T]
    final case class Failure[T](ex: Throwable) extends Response[T]

    def success[T](t: T): Response[T] = Success(t)
  }
}
