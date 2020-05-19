package not.ogame.bots.ghostbuster

import cats.effect.{ExitCode, IO, IOApp}
import not.ogame.bots.Credentials
import not.ogame.bots.SuppliesBuilding.MetalMine
import not.ogame.bots.selenium.SeleniumOgameDriverCreator
import cats.implicits._
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val credentials = ConfigSource.file(s"${System.getenv("HOME")}/.not-ogame-bots/credentials.conf").loadOrThrow[Credentials]
    new SeleniumOgameDriverCreator()
      .create(credentials)
      .use(
        ogame =>
          ogame.login() >>
            ogame.readSuppliesPage("33794124").map(println(_)) >>
            ogame.buildSuppliesBuilding("33794124", MetalMine) >>
            ogame.readSuppliesPage("33794124").map(println(_)) >>
            IO.never
      )
      .as(ExitCode.Success)
  }
}
