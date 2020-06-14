package not.ogame.bots.selenium

import not.ogame.bots._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

class SendFleetActionSpec extends munit.FunSuite {
  implicit val clock: LocalClock = new RealLocalClock()

  private val driverFixture = FunFixture[WebDriver](
    setup = { _ =>
      new FirefoxDriver()
    },
    teardown = { driver =>
      driver.close()
    }
  )

  driverFixture.test("Should read planet list") { driver =>
    driver.get(getClass.getResource("/send_fleet_action/send_fleet.html").toURI.toString)
    testSendFleet(driver)
    Thread.sleep(10_000)
  }

  private def testSendFleet(driver: WebDriver): Unit =
    new SendFleetAction(driver).sendFleet(
      SendFleetRequest(
        from = PlayerPlanet(PlanetId.apply(""), Coordinates(0, 0, 0)),
        targetCoordinates = Coordinates(1, 1, 16),
        ships = SendFleetRequestShips.AllShips,
        fleetMissionType = FleetMissionType.Expedition,
        resources = FleetResources.Max,
        speed = FleetSpeed.Percent100
      )
    )
}
