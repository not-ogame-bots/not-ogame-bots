package not.ogame.bots.ordon

import java.time.{Clock, LocalDateTime}

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import not.ogame.bots.OgameDriver
import not.ogame.bots.selenium.SeleniumOgameDriverCreator

import scala.concurrent.duration._

object OrdonMain extends IOApp {
  private implicit val clock: Clock = Clock.systemUTC()

  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    restartOnError(runBot)
  }

  private def restartOnError(function: () => IO[ExitCode]): IO[ExitCode] = {
    function().flatMap(exitCode => if (exitCode != ExitCode.Success) restartOnError(function) else IO.pure(exitCode))
  }

  private def runBot(): IO[ExitCode] = {
    new SeleniumOgameDriverCreator()
      .create(OrdonConfig.getCredentials)
      .use { ogame =>
        ogame.login() >> process(ogame, OrdonConfig.getInitialActions)
      }
      .as(ExitCode.Success)
  }

  @scala.annotation.tailrec
  private def process(ogame: OgameDriver[IO], scheduled: IO[List[ScheduledAction[IO]]]): IO[Unit] = {
    println("Process")
    val scheduledActionList = scheduled.unsafeRunSync()
    val processed: IO[List[ScheduledAction[IO]]] = scheduledActionList.map(_.process(ogame, LocalDateTime.now())).sequence.map(_.flatten)
    val withSleep = processed
      .flatMap(a => IO.sleep(1 second).map(_ => a))
    process(ogame, withSleep)
  }
}
