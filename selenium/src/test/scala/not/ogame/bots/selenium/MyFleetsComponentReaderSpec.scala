package not.ogame.bots.selenium

import not.ogame.bots.CoordinatesType.{Moon, Planet}
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots.ShipType.{Destroyer, LargeCargoShip, LightFighter}
import not.ogame.bots._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

class MyFleetsComponentReaderSpec extends munit.FunSuite with GecoDriver {
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
    val myFleetPage = testReadAllFleets(driver)
    val allFleets = myFleetPage.fleets
    assertEquals(allFleets.size, 7)
    val firstFleet = allFleets.head
    assertEquals(firstFleet.fleetId, FleetId.apply("fleet2075500"))
    assertEquals(firstFleet.arrivalTime.getHour, 15)
    assertEquals(firstFleet.arrivalTime.getMinute, 33)
    assertEquals(firstFleet.arrivalTime.getSecond, 56)
    assertEquals(firstFleet.fleetMissionType, FleetMissionType.Expedition)
    assertEquals(firstFleet.from, Coordinates(3, 133, 5, Moon))
    assertEquals(firstFleet.isReturning, true)
    assertEquals(firstFleet.to, Coordinates(3, 133, 16, Planet))
    val lastFleet = allFleets(6)
    assertEquals(lastFleet.fleetId, FleetId.apply("fleet2081270"))
    assertEquals(lastFleet.fleetMissionType, Deployment)
    assertEquals(lastFleet.from, Coordinates(3, 133, 6, Planet))
    assertEquals(lastFleet.to, Coordinates(3, 133, 6, Moon))
    assertEquals(lastFleet.isReturning, false)
    assertEquals(lastFleet.ships(LightFighter), 13766)
    assertEquals(lastFleet.ships(Destroyer), 3172)
    assertEquals(lastFleet.ships(LargeCargoShip), 0)
    assertEquals(myFleetPage.fleetSlots.currentFleets, 7)
    assertEquals(myFleetPage.fleetSlots.maxFleets, 14)
    assertEquals(myFleetPage.fleetSlots.currentExpeditions, 6)
    assertEquals(myFleetPage.fleetSlots.maxExpeditions, 6)
  }

  private def testReadAllFleets(driver: WebDriver): MyFleetPageData =
    new MyFleetsComponentReader(driver).readMyFleets()
}
