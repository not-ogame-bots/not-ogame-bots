package not.ogame.bots.ghostbuster.processors

import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots.PlayerPlanet
import not.ogame.bots.ghostbuster.{BotConfig, FLogger}

import scala.concurrent.duration._

class BuilderProcessor(botConfig: BotConfig, taskExecutor: TaskExecutor) extends FLogger {
  private val builder = new Builder(taskExecutor, botConfig)

  def run(): Task[Unit] = {
    if (botConfig.smartBuilder) {
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
        case None                   => Logger[Task].info("Cannot build anything right now, sleeping for 10 minutes") >> Task.sleep(10 minutes)
      }
      .flatMap(_ => loopBuilder(planet))
  }
}
