package not.ogame.bots.selenium

import not.ogame.bots.CoordinatesType.{Moon, Planet}
import not.ogame.bots.FleetAttitude.Friendly
import not.ogame.bots.FleetMissionType.{Deployment, Expedition}
import not.ogame.bots.{Coordinates, Fleet, LocalClock, RealLocalClock}
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

class AllFleetsComponentReaderSpec extends munit.FunSuite {
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
    driver.get(getClass.getResource("/all_fleets_component_reader/events.html").toURI.toString)
    val allFleets = testReadAllFleets(driver)
    assertEquals(allFleets.size, 11)
    val firstFleet = allFleets.head
    //    assertEquals(firstFleet.arrivalTime.getHour, 13)
    //    assertEquals(firstFleet.arrivalTime.getMinute, 29)
    //    assertEquals(firstFleet.arrivalTime.getSecond, 40)
    assertEquals(firstFleet.fleetAttitude, Friendly)
    assertEquals(firstFleet.fleetMissionType, Deployment)
    assertEquals(firstFleet.from, Coordinates(3, 133, 6, Planet))
    assertEquals(firstFleet.isReturning, false)
    assertEquals(firstFleet.to, Coordinates(3, 133, 6, Moon))
    val secondFleet = allFleets(1)
    assertEquals(secondFleet.fleetMissionType, Expedition)
    assertEquals(secondFleet.from, Coordinates(3, 133, 5, Moon))
    assertEquals(secondFleet.to, Coordinates(3, 133, 16, Planet))
    assertEquals(secondFleet.isReturning, true)
  }

  private def testReadAllFleets(driver: WebDriver): List[Fleet] =
    new AllFleetsComponentReader(driver).readAllFleets()
}
