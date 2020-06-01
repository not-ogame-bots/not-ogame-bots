package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import not.ogame.bots.selenium.{OgameUrlProvider, SeleniumOgameDriverCreator}
import not.ogame.bots.{LocalClock, OgameDriver, RealLocalClock}

import scala.concurrent.duration._

object OrdonMain extends IOApp {
  private implicit val clock: LocalClock = new RealLocalClock()

  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    runBot()
  }

  private def runBot(): IO[ExitCode] = {
    new SeleniumOgameDriverCreator[IO]()
      .create(OrdonConfig.getCredentials, new OgameUrlProvider(OrdonConfig.getCredentials))
      .use { ogame =>
        ogame.login() >> process(ogame, OrdonConfig.getInitialActions)
      }
      .as(ExitCode.Success)
      .handleErrorWith(throwable => {
        println(throwable)
        runBot()
      })
  }

  @scala.annotation.tailrec
  private def process(ogame: OgameDriver[IO], scheduled: IO[List[ScheduledAction[IO]]]): IO[Unit] = {
    val scheduledActionList = scheduled.unsafeRunSync()
    val processed: IO[List[ScheduledAction[IO]]] = scheduledActionList.map(process(_, ogame, clock.now())).sequence.map(_.flatten)
    val withSleep = processed
      .map(newList => printState(scheduledActionList, newList))
      //TODO sleep to next action
      .flatMap(a => IO.sleep(1 second).map(_ => a))
    process(ogame, withSleep)
  }

  private def printState(scheduledActionList: List[ScheduledAction[IO]], newList: List[ScheduledAction[IO]]): List[ScheduledAction[IO]] = {
    if (scheduledActionList != newList) {
      println("Actions changed:" + clock.now())
      newList.foreach(println(_))
    }
    newList
  }

  private def process(scheduledAction: ScheduledAction[IO], ogame: OgameDriver[IO], now: ZonedDateTime): IO[List[ScheduledAction[IO]]] = {
    if (scheduledAction.resumeOn.isBefore(now)) {
      scheduledAction.action.process(ogame)
    } else {
      IO.pure(List(scheduledAction))
    }
  }
}
