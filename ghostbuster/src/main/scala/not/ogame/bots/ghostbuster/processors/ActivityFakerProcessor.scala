package not.ogame.bots.ghostbuster.processors

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.reactive.{Consumer, Observable}
import not.ogame.bots.OgameDriver
import not.ogame.bots.ghostbuster.FLogger
import not.ogame.bots.ghostbuster.executor.{OgameActionExecutor, _}
import not.ogame.bots.ghostbuster.notifications.Notifier
import not.ogame.bots.ghostbuster.ogame.OgameAction

import scala.concurrent.duration._
import scala.util.Random

class ActivityFakerProcessor(ogameActionDriver: OgameDriver[OgameAction], notifier: Notifier)(implicit executor: OgameActionExecutor[Task])
    extends FLogger {
  def run(): Task[Unit] = {
    notifier.subscribeToNotifications
      .switchMap { _ =>
        Observable.intervalAtFixedRate(14 minutes, 14 minutes)
      }
      .consumeWith(Consumer.foreachTask(_ => checkSomething))
  }

  private def checkSomething = {
    Logger[Task].info("activity faker running...") >>
      ogameActionDriver
        .readPlanets()
        .flatMap { planets =>
          Random.shuffle(planets).take(planets.size / 2 + 1).map(it => ogameActionDriver.readSuppliesPage(it.id).void).sequence
        }
        .execute()
        .void
  }
}
