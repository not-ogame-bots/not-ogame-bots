package not.ogame.bots.selenium

import not.ogame.bots.CoordinatesType.Moon
import not.ogame.bots.selenium.PlayerActivity.{LessThan15MinutesAgo, MinutesAgo, NotActive}
import not.ogame.bots.{Coordinates, LocalClock, RealLocalClock}
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

class GalaxyComponentReaderSpec extends munit.FunSuite {
  implicit val clock: LocalClock = new RealLocalClock()

  private val driverFixture = FunFixture[WebDriver](
    setup = { _ =>
      new FirefoxDriver()
    },
    teardown = { driver =>
      driver.close()
    }
  )

  driverFixture.test("Should read galaxy page") { driver =>
    driver.get(getClass.getResource("/galaxy_component_reader/galaxy.html").toURI.toString)
    val galaxyPage = testReadGalaxy(driver)
    assertEquals(galaxyPage(Coordinates(3, 133, 4)), LessThan15MinutesAgo)
    assertEquals(galaxyPage(Coordinates(3, 133, 4, Moon)), NotActive)
    assertEquals(galaxyPage(Coordinates(3, 133, 5)), MinutesAgo(47))
  }

  private def testReadGalaxy(driver: WebDriver): Map[Coordinates, PlayerActivity] =
    new GalaxyComponentReader(driver).readGalaxyPage()
}
