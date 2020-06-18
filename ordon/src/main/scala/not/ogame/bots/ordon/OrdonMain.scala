package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import not.ogame.bots.ordon.utils.Noise
import not.ogame.bots.selenium.{OgameUrlProvider, SeleniumOgameDriverCreator}
import not.ogame.bots.{LocalClock, OgameDriver, RealLocalClock}

import scala.concurrent.duration._

object OrdonMain extends IOApp {
  private val ordonConfig = OrdonTestConfig
  private implicit val clock: LocalClock = new RealLocalClock()
  private var lastClockUpdate: ZonedDateTime = clock.now()
  private var errors: List[ZonedDateTime] = List()

  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    System.setProperty("webdriver.chrome.driver", "selenium/chromedriver-v83")
    runBot()
  }

  def addError(time: ZonedDateTime): Unit = {
    errors = errors ++ List(time)
  }

  private def runBot(): IO[ExitCode] = {
    if (lastClockUpdate.isBefore(clock.now().minusMinutes(4))) {
      println("lastClockUpdate" + lastClockUpdate)
      Noise.makeNoise()
    }
    if (errors.count(error => error.isAfter(clock.now().minusMinutes(4))) > 3) {
      println("errors")
      errors.filter(error => error.isAfter(clock.now().minusMinutes(4))).foreach(println(_))
      Noise.makeNoise()
    }
    new SeleniumOgameDriverCreator[IO](new OgameUrlProvider(ordonConfig.getCredentials))
      .create(ordonConfig.getCredentials)
      .use { ogame =>
        ogame.login() >> process(ogame, ordonConfig.getInitialActions)
      }
      .as(ExitCode.Success)
      .handleErrorWith(throwable => {
        addError(clock.now())
        println("Fatal error while running bot:")
        println(throwable)
        throwable.printStackTrace()
        println("Restarting bot from scratch...")
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
    lastClockUpdate = clock.now()
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
