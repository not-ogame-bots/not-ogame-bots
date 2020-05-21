package not.ogame.bots.ordon

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import not.ogame.bots.Credentials
import not.ogame.bots.selenium.SeleniumOgameDriverCreator
import pureconfig.ConfigSource

object OrdonMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val credentials = ConfigSource.file(s"${System.getenv("HOME")}/.not-ogame-bots/credentials-fire.conf").loadOrThrow[Credentials]
    new SeleniumOgameDriverCreator()
      .create(credentials)
      .use { ogame =>
        ogame.login() >> ogame.readSuppliesPage("33794124").map(println(_))
      }
      .as(ExitCode.Success)
  }
}
