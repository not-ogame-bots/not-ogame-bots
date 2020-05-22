package not.ogame.bots.ghostbuster

import java.time.Clock

import cats.MonadError
import cats.effect.Timer
import cats.implicits._
import com.softwaremill.quicklens._
import not.ogame.bots.{OgameDriver, SuppliesBuilding}
import not.ogame.bots.ghostbuster.PlanetState.LoggedIn
import not.ogame.bots.ghostbuster.TaskExecutor._

class TaskExecutor[F[_]: MonError: Timer](ogameDriver: OgameDriver[F], gBot: GBot)(implicit clock: Clock) {
  def execute(state: PlanetState): F[PlanetState.LoggedIn] = {
    state match {
      case loggedOut: PlanetState.LoggedOut =>
        logIn(loggedOut)
          .map(gBot.nextStep)
      case loggedIn: PlanetState.LoggedIn =>
        executeAl(loggedIn.scheduledTasks, loggedIn).map(s => s: PlanetState.LoggedIn)
    }
  }

  private def logIn(state: PlanetState): F[PlanetState.LoggedIn] = {
    ogameDriver.login() >> ogameDriver
      .readSuppliesPage(PlanetId)
      .map { suppliesPage =>
        PlanetState.LoggedIn(suppliesPage, state.scheduledTasks)
      }
      .handleErrorWith { e =>
        e.printStackTrace()
        logIn(state)
      }
  }

  private def executeAl(tasks: List[Task], state: LoggedIn): F[PlanetState.LoggedIn] = {
    tasks match {
      case ::(head, next) if clock.instant().isAfter(head.executeAfter) =>
        execute(head, state)
          .handleErrorWith { e =>
            e.printStackTrace()
            logIn(state)
          }
          .flatMap { s =>
            executeAl(
              next,
              gBot.nextStep(
                s.modify(_.scheduledTasks)
                  .setTo(state.scheduledTasks.filterNot(_ == head))
              )
            )
          }
      case ::(_, next) => executeAl(next, state)
      case Nil         => state.pure[F]
    }
  }

  private def execute(task: Task, state: LoggedIn): F[LoggedIn] = {
    task match {
      case Task.BuildSupply(suppliesBuilding, level, executeAfter) =>
        buildSupplyBuilding(state, suppliesBuilding)
      case Task.Refresh(_) =>
        ogameDriver.readSuppliesPage(PlanetId).map(state.modify(_.suppliesPage).setTo(_))
    }
  }

  private def buildSupplyBuilding(state: LoggedIn, suppliesBuilding: SuppliesBuilding) = {
    //TODO check if level is correct
    //TODO move executeAfter outside of task?
    //TODO check resources
    for {
      _ <- ogameDriver.buildSuppliesBuilding(PlanetId, suppliesBuilding)
      newSuppliesPage <- ogameDriver.readSuppliesPage(PlanetId)
    } yield {
      state
        .modify(_.suppliesPage)
        .setTo(newSuppliesPage)
    }
  }
}

object TaskExecutor {
  type MonError[F[_]] = MonadError[F, Throwable]
  val PlanetId = "33653280"
}
