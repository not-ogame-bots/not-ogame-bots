package not.ogame.bots.selenium

import not.ogame.bots.CoordinatesType.{Moon, Planet}
import not.ogame.bots.FleetMissionType.{Deployment, Unknown}
import not.ogame.bots.{Coordinates, LocalClock, MyFleet, RealLocalClock}
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

class MyFleetsComponentReaderSpec extends munit.FunSuite {
  implicit val clock: LocalClock = new RealLocalClock()

  private val driverFixture = FunFixture[WebDriver](
    setup = { _ =>
      new FirefoxDriver()
    },
    teardown = { driver =>
      driver.close()
    }
  )

  driverFixture.test("Should read my fleets list") { driver =>
    driver.get(getClass.getResource("/my_fleets_component_reader/my_fleets.html").toURI.toString)
    val allFleets = testReadAllFleets(driver)
    assertEquals(allFleets.size, 7)
    val firstFleet = allFleets.head
    assertEquals(firstFleet.fleetId, "fleet2075500")
    assertEquals(firstFleet.arrivalTime.getHour, 15)
    assertEquals(firstFleet.arrivalTime.getMinute, 33)
    assertEquals(firstFleet.arrivalTime.getSecond, 56)
    assertEquals(firstFleet.fleetMissionType, Unknown)
    assertEquals(firstFleet.from, Coordinates(3, 133, 5, Moon))
    assertEquals(firstFleet.isReturning, true)
    assertEquals(firstFleet.to, Coordinates(3, 133, 16, Planet))
    val lastFleet = allFleets(6)
    assertEquals(lastFleet.fleetId, "fleet2081270")
    assertEquals(lastFleet.fleetMissionType, Deployment)
    assertEquals(lastFleet.from, Coordinates(3, 133, 6, Planet))
    assertEquals(lastFleet.to, Coordinates(3, 133, 6, Moon))
    assertEquals(lastFleet.isReturning, false)
  }

  private def testReadAllFleets(driver: WebDriver): List[MyFleet] =
    new MyFleetsComponentReader(driver).readMyFleets()
}
