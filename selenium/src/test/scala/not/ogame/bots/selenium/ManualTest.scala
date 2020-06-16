package not.ogame.bots.selenium

import cats.effect.{ExitCode, IO, IOApp}
import not.ogame.bots._

object ManualTest extends IOApp {
  private implicit val clock: LocalClock = new RealLocalClock()

  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val testCredentials = Credentials("fire@fire.pl", "1qaz2wsx", "Mensa", "s165-pl")
    WebDriverResource
      .firefox[IO]()
      .map(
        driver =>
          new SeleniumOgameDriverCreator[IO](driver)
            .create(testCredentials)
      )
      .use(manualTestCase)
      .as(ExitCode.Success)
  }

  private def manualTestCase(ogame: OgameDriver[IO]): IO[Unit] =
    for {
      _ <- ogame.login()
      _ <- ogame.sendFleet(
        SendFleetRequest(
          from = PlayerPlanet(PlanetId.apply("33794124"), Coordinates(7, 258, 10)),
          targetCoordinates = Coordinates(7, 257, 12),
          ships = SendFleetRequestShips.AllShips,
          fleetMissionType = FleetMissionType.Transport,
          resources = FleetResources.Max,
          speed = FleetSpeed.Percent10
        )
      )
      myFleets <- ogame.readMyFleets()
      _ <- ogame.returnFleet(myFleets.fleets.head.fleetId)
    } yield ()
}
