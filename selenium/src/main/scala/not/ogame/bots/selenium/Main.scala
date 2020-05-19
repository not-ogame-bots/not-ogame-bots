package not.ogame.bots.selenium

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import not.ogame.bots.Credentials
import not.ogame.bots.SuppliesBuilding.MetalMine

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    new SeleniumOgameDriverCreator()
      .create(Credentials("fire@fire.pl", "1qaz2wsx", "Mensa", "s165-pl"))
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
