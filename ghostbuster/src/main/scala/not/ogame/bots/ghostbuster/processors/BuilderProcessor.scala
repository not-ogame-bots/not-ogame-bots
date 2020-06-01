package not.ogame.bots.ghostbuster.processors

import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots.{LocalClock, PlayerPlanet}
import not.ogame.bots.ghostbuster.{FLogger, SmartBuilderConfig}
import cats.implicits._

class BuilderProcessor(builder: Builder, smartBuilder: SmartBuilderConfig, taskExecutor: TaskExecutor)(implicit clock: LocalClock)
    extends FLogger {
  def run(): Task[Unit] = {
    if (smartBuilder.isOn) {
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
        case Some(buildingFinished) => taskExecutor.waitTo(buildingFinished)
        case None =>
          taskExecutor
            .readAllFleets()
            .map(_.filter(f => f.to == planet.coordinates))
            .flatMap { fleets =>
              val waitTo = (fleets.map(_.arrivalTime) :+ clock.now().plus(smartBuilder.interval)).min
              Logger[Task].info(s"Cannot build anything right now, waiting till $waitTo") >> taskExecutor.waitTo(waitTo)
            }
      }
      .flatMap(_ => loopBuilder(planet))
  }
}
