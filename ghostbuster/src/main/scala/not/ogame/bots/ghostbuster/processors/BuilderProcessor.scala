package not.ogame.bots.ghostbuster.processors

import monix.eval.Task
import not.ogame.bots.PlayerPlanet
import not.ogame.bots.ghostbuster.BotConfig
import scala.concurrent.duration._

class BuilderProcessor(botConfig: BotConfig, taskExecutor: TaskExecutor) {
  private val builder = new Builder(taskExecutor, botConfig)

  def run(): Task[List[Unit]] = {
    taskExecutor
      .readPlanets()
      .flatMap { planets =>
        Task.parSequence(planets.map(loopBuilder))
      }
  }

  private def loopBuilder(planet: PlayerPlanet): Task[Unit] = {
    builder
      .buildNextThingFromWishList(planet)
      .flatMap {
        case Some(buildingFinished) => taskExecutor.waitTo(buildingFinished)
        case None                   => Task.eval(println("Cannot build anything right now, sleeping for 10 minutes")) >> Task.sleep(10 minutes)
      }
      .flatMap(_ => loopBuilder(planet))
  }
}