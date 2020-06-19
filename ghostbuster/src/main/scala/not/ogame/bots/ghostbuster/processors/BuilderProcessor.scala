package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots.{LocalClock, PlayerPlanet}
import not.ogame.bots.ghostbuster.{FLogger, SmartBuilderConfig}
import cats.implicits._

class BuilderProcessor(builder: Builder, config: SmartBuilderConfig, taskExecutor: TaskExecutor)(implicit clock: LocalClock)
    extends FLogger {
  def run(): Task[Unit] = {
    if (config.isOn) {
      taskExecutor
        .readPlanets()
        .flatMap(planets => Task.parSequence(planets.map(loopBuilder)))
        .void
        .onError(e => Logger[Task].error(s"restarting building processor ${e.getMessage}"))
        .onErrorRestartIf(_ => true)
    } else {
      Task.never
    }
  }

  private def loopBuilder(planet: PlayerPlanet): Task[Unit] = {
    builder
      .buildNextThingFromWishList(planet)
      .flatMap {
        case BuilderResult.Building(finishTime) =>
          Logger[Task].info(s"Waiting for building to finish til $finishTime") >>
            taskExecutor.waitTo(finishTime)
        case BuilderResult.Waiting(productionTime) =>
          firstFleetArrivalTime(planet).flatMap {
            case Some(arrivalTime) if arrivalTime.isBefore(productionTime) =>
              Logger[Task].info(s"Waiting for first fleet to arrive til $productionTime") >>
                taskExecutor.waitTo(arrivalTime)
            case None =>
              Logger[Task].info(s"Waiting for resources to produce til $productionTime") >>
                taskExecutor.waitTo(productionTime)
          }

        case BuilderResult.Idle => taskExecutor.waitTo(clock.now().plus(config.interval))
      }
      .flatMap(_ => loopBuilder(planet))
  }

  private def firstFleetArrivalTime(planet: PlayerPlanet) = {
    taskExecutor
      .readAllFleets()
      .map(_.filter(f => f.to == planet.coordinates))
      .map {
        case l if l.nonEmpty => l.map(_.arrivalTime).min.some
        case Nil             => Option.empty[ZonedDateTime]
      }
  }
}
