package not.ogame.bots.ghostbuster.executor.impl

import java.time.ZonedDateTime
import java.util.UUID

import cats.effect.concurrent.MVar
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import not.ogame.bots._
import not.ogame.bots.ghostbuster.{FLogger, processors}
import not.ogame.bots.ghostbuster.interpreter.TaskExecutor

import scala.concurrent.duration._

class TaskExecutorImpl(ogameDriver: OgameDriver[Task])(implicit clock: LocalClock)
    extends TaskExecutor[Task]
    with FLogger
    with StrictLogging {
  type Channel[A] = MVar[Task, A]

  private val requests: Channel[Request[_]] = MVar[Task].empty[Request[_]].runSyncUnsafe()

  def run(): Task[Unit] = {
    safeLogin() >> processNextAction()
  }

  private def processNextAction(): Task[Unit] = {
    for {
      action <- requests.take
      _ <- safeHandleAction(action)
      _ <- processNextAction()
    } yield ()
  }

  private def safeHandleAction[T](request: Request[T]): Task[Unit] = {
    handleAction(request)
      .handleErrorWith { e =>
        for {
          _ <- Logger[Task].error(e)(e.getMessage)
          isStillLogged <- ogameDriver.checkIsLoggedIn()
          _ <- if (isStillLogged) {
            val response = Response.Failure[T](e)
            Logger[Task].warn("still logged, failing action...") >>
              Logger[Task].debug(s"action response: ${pprint.apply(response)}") >>
              request.response.put(response)
          } else {
            Logger[Task].warn("not logged") >>
              safeLogin >> safeHandleAction(request)
          }
        } yield ()
      }
  }

  private def safeLogin(): Task[Unit] = {
    ogameDriver
      .login()
      .handleErrorWith { e =>
        Logger[Task].error(e)("Login failed, retrying in 2 seconds") >> Task.sleep(2 seconds) >> Task.raiseError(e)
      }
      .onErrorRestart(5)
  }

  private def handleAction[T](request: Request[T]) = {
    request.action
      .flatTap(response => Logger[Task].debug(s"action response: ${pprint.apply(response)}"))
      .flatMap(r => request.response.put(Response.success(r)))
  }

  def exec[T](action: Task[T]): Task[T] = {
    Task
      .eval(UUID.randomUUID())
      .flatMap(uuid => Request[T](withLogAndTime(action, uuid)))
      .flatTap(r => requests.put(r))
      .flatMap(r => r.response.take)
      .flatMap {
        case Response.Success(value) =>
          Task.pure(value)
        case Response.Failure(ex) =>
          Task.raiseError[T](ex)
      }
  }

  private def withLogAndTime[T](action: Task[T], uuid: UUID) = {
    Task.eval(clock.now()).flatMap { startTime =>
      Logger[Task].debug(s"transaction start: $uuid") >> action <* Logger[Task]
        .debug(s"transaction end: $uuid ${processors.timeDiff(startTime, clock.now())}")
    }
  }
}
