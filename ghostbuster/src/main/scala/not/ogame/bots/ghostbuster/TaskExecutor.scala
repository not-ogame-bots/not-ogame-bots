package not.ogame.bots.ghostbuster

import java.time.Clock

import cats.MonadError
import not.ogame.bots.OgameDriver
import cats.implicits._
import com.softwaremill.quicklens._
import not.ogame.bots.ghostbuster.TaskExecutor._

class TaskExecutor[F[_]: MonError](ogameDriver: OgameDriver[F])(implicit clock: Clock) {
  def execute(state: State): F[State] = {
    state match {
      case loggedOut: State.LoggedOut =>
        logIn(loggedOut)
      case loggedIn: State.LoggedIn =>
        loggedIn.scheduledTasks
          .collectFirst {
            case readyTask if clock.instant().isAfter(readyTask.executeAfter) =>
              readyTask match {
                case Task.Build(suppliesBuilding, level, executeAfter) =>
                  //TODO check if level is correct
                  //TODO move executeAfter outside of task?
                  //TODO check resources
                  ogameDriver.buildSuppliesBuilding(PlanetId, suppliesBuilding).map { _ =>
                    loggedIn
                      .modify(_.scheduledTasks)
                      .setTo(loggedIn.scheduledTasks.filterNot(_ == readyTask)): State
                  }
                case Task.Login(_) =>
                  new IllegalStateException("We are already logged!!! Cant login again.").raiseError[F, State]
                case Task.Refresh(_) =>
                  ogameDriver.readSuppliesPage(PlanetId).map { suppliesPage =>
                    loggedIn.modify(_.suppliesPage).setTo(suppliesPage): State
                  }
              }
          }
          .getOrElse((loggedIn: State).pure[F])
    } // TODO handle error assuming we have been logged out
  }

  private def logIn(loggedOut: State.LoggedOut): F[State] = {
    loggedOut.scheduledTasks
      .collectFirst {
        case loginTask: Task.Login if clock.instant().isAfter(loginTask.executeAfter) =>
          ogameDriver.login() >> ogameDriver.readSuppliesPage(PlanetId).map { suppliesPage =>
            State.loggedIn(suppliesPage, loggedOut.wishList, loggedOut.scheduledTasks.filterNot(_ == loginTask))
          }
      }
      .getOrElse((loggedOut: State).pure[F])
  }
}

object TaskExecutor {
  type MonError[F[_]] = MonadError[F, Throwable]
  val PlanetId = "33653280"
}
