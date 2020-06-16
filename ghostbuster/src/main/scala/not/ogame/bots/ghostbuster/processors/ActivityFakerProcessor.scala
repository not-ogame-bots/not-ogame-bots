package not.ogame.bots.ghostbuster.processors

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.reactive.{Consumer, Observable}
import not.ogame.bots.ghostbuster.FLogger

import scala.concurrent.duration._
import scala.util.Random

class ActivityFakerProcessor(taskExecutor: TaskExecutor) extends FLogger {
  def run(): Task[Unit] = {
    taskExecutor.subscribeToNotifications
      .switchMap { _ =>
        Observable.intervalAtFixedRate(14 minutes, 14 minutes)
      }
      .consumeWith(Consumer.foreachTask(_ => checkSomething))
  }

  private def checkSomething = {
    Logger[Task].info("activity faker running...") >>
      taskExecutor
        .readPlanetsAndMoons()
        .flatMap { planets =>
          Random.shuffle(planets).take(planets.size / 2 + 1).map(it => taskExecutor.readSupplyPage(it).void).sequence
        }
        .void
  }
}
