package not.ogame.bots.ghostbuster.processors

import cats.implicits._
import monix.eval.Task

import scala.concurrent.duration._

class ActivityFakerProcessor(taskExecutor: TaskExecutor) {
  def run(): Task[Unit] = {
    Task.eval(println("activity faker sleeping...")) >> Task.sleep(14 minutes) >> checkSomething >> run()
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
