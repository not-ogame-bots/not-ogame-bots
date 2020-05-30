package not.ogame.bots.ghostbuster.processors

import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots.PlayerPlanet
import not.ogame.bots.ghostbuster.{FLogger, SmartBuilderConfig}

class BuilderProcessor(builder: Builder, smartBuilder: SmartBuilderConfig, taskExecutor: TaskExecutor) extends FLogger {
  def run(): Task[Unit] = {
    if (smartBuilder.isOn) {
      taskExecutor
        .readPlanets()
        .flatMap(planets => Task.parSequence(planets.map(loopBuilder)))
        .void
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
          Logger[Task].info(s"Cannot build anything right now, sleeping for ${smartBuilder.interval}") >> Task.sleep(smartBuilder.interval)
      }
      .flatMap(_ => loopBuilder(planet))
  }
}
