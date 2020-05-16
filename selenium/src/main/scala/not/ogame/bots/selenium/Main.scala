package not.ogame.bots.selenium

import cats.effect.{ExitCode, IO, IOApp}
import not.ogame.bots.Credentials

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val ogameDriver = new SeleniumOgameDriverCreator().create(Credentials("fire@fire.pl", "1qaz2wsx", "Mensa", "s165-pl"))
    ogameDriver.login().as(ExitCode.Success)
  }
}
