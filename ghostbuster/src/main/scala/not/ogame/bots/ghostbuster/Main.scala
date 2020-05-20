package not.ogame.bots.ghostbuster

import java.time.Clock

import cats.effect.{ExitCode, IO, IOApp}
import not.ogame.bots.Credentials
import not.ogame.bots.selenium.SeleniumOgameDriverCreator
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import cats.implicits._
import scala.concurrent.duration._

object Main extends IOApp {
  private implicit val clock: Clock = Clock.systemUTC()

  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val botConfig = ConfigSource.default.loadOrThrow[BotConfig]
    val credentials = ConfigSource.file(s"${System.getenv("HOME")}/.not-ogame-bots/credentials.conf").loadOrThrow[Credentials]
    val gbot = new GBot(RealRandomTimeJitter)
    new SeleniumOgameDriverCreator()
      .create(credentials)
      .use { ogame =>
        val taskExecutor = new TaskExecutor[IO](ogame)
        def loop(state: State): IO[State] = {
          val ns = gbot.nextStep(state)
          taskExecutor.execute(ns).flatMap(s => IO.sleep(1 seconds) >> loop(s))
        }
        loop(State.LoggedOut(List.empty, botConfig.wishlist))
      }
      .as(ExitCode.Success)
  }
}
