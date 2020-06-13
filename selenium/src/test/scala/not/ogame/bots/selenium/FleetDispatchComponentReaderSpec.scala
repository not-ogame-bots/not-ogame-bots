package not.ogame.bots.selenium

import not.ogame.bots._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

class FleetDispatchComponentReaderSpec extends munit.FunSuite {
  implicit val clock: LocalClock = new RealLocalClock()

  private val driverFixture = FunFixture[WebDriver](
    setup = { _ =>
      new FirefoxDriver()
    },
    teardown = { driver =>
      driver.close()
    }
  )

  driverFixture.test("Should read fleet slots") { driver =>
    driver.get(getClass.getResource("/fleet_dispatch_component_reader/fleet_dipatch.html").toURI.toString)
    val fleetSlots = testRead(driver)
    assertEquals(fleetSlots.currentFleets, 7)
    assertEquals(fleetSlots.maxFleets, 17)
    assertEquals(fleetSlots.currentExpeditions, 6)
    assertEquals(fleetSlots.maxExpeditions, 6)
    assertEquals(fleetSlots.currentTradeFleets, 10)
    assertEquals(fleetSlots.maxTradeFleets, 15)
  }

  private def testRead(driver: WebDriver): FleetSlots =
    new FleetDispatchComponentReader(driver).readSlots()
}
