package not.ogame.bots.selenium

import cats.effect.{ExitCode, IO, IOApp}
import not.ogame.bots.Credentials
import cats.implicits._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    new SeleniumOgameDriverCreator()
      .create(Credentials("fire@fire.pl", "1qaz2wsx", "Mensa", "s165-pl"))
      .use(ogame => ogame.login() >> IO.never)
      .as(ExitCode.Success)
  }
}
