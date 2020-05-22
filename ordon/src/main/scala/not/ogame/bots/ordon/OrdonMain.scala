package not.ogame.bots.ordon

import java.time.Clock

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import not.ogame.bots.FleetMissionType.Expedition
import not.ogame.bots.SendFleetRequestShips.Ships
import not.ogame.bots.ShipType.SmallCargoShip
import not.ogame.bots.selenium.SeleniumOgameDriverCreator
import not.ogame.bots.{Coordinates, Credentials, Resources, SendFleetRequest}

object OrdonMain extends IOApp {
  private implicit val clock: Clock = Clock.systemUTC()

  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val credentials = Credentials("fire@fire.pl", "1qaz2wsx", "Mensa", "s165-pl")
    val sendFleetRequest =
      SendFleetRequest("33794124", Ships(Map(SmallCargoShip -> 1)), Coordinates(7, 258, 16), Expedition, Resources(0, 0, 0))
    new SeleniumOgameDriverCreator()
      .create(credentials)
      .use { ogame =>
        ogame.login() >> ogame.sendFleet(sendFleetRequest) >> IO.never
      }
      .as(ExitCode.Success)
  }
}
