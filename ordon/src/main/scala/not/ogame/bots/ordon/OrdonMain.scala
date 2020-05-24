package not.ogame.bots.ordon

import java.time.{Clock, Instant}

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import not.ogame.bots.OgameDriver
import not.ogame.bots.selenium.SeleniumOgameDriverCreator

import scala.concurrent.duration._

object OrdonMain extends IOApp {
  private implicit val clock: Clock = Clock.systemUTC()

  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    runBot()
  }

  private def runBot(): IO[ExitCode] = {
    new SeleniumOgameDriverCreator[IO]()
      .create(OrdonConfig.getCredentials)
      .use { ogame =>
        ogame.login() >> process(ogame, OrdonConfig.getInitialActions)
      }
      .as(ExitCode.Success)
      .handleErrorWith(_ => runBot())
  }

  @scala.annotation.tailrec
  private def process(ogame: OgameDriver[IO], scheduled: IO[List[ScheduledAction[IO]]]): IO[Unit] = {
    println("Process")
    val scheduledActionList = scheduled.unsafeRunSync()
    val processed: IO[List[ScheduledAction[IO]]] = scheduledActionList.map(process(_, ogame, Instant.now())).sequence.map(_.flatten)
    val withSleep = processed
      .flatMap(a => IO.sleep(1 second).map(_ => a))
    process(ogame, withSleep)
  }

  private def process(scheduledAction: ScheduledAction[IO], ogame: OgameDriver[IO], now: Instant): IO[List[ScheduledAction[IO]]] = {
    if (scheduledAction.resumeOn.isBefore(now)) {
      scheduledAction.action.process(ogame)
    } else {
      IO.pure(List(scheduledAction))
    }
  }
}
