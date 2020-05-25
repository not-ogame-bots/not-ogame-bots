package not.ogame.bots.ghostbuster.processors

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots.ghostbuster.FLogger

import scala.concurrent.duration._

class ActivityFakerProcessor(taskExecutor: TaskExecutor) extends FLogger {
  def run(): Task[Unit] = {
    Logger[Task].info("activity faker sleeping...") >> Task.sleep(14 minutes) >> checkSomething >> run()
  }

  private def checkSomething = {
    taskExecutor
      .readPlanets()
      .flatMap { planets =>
        planets
          .take(2)
          .map { planet =>
            taskExecutor.readSupplyPage(planet)
          }
          .sequence
      }
      .void
  }
}
