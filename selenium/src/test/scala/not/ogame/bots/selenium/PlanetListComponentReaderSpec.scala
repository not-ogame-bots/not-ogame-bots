package not.ogame.bots.selenium

import not.ogame.bots.{Coordinates, PlanetId, PlayerPlanet}
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

class PlanetListComponentReaderSpec extends munit.FunSuite {
  private val driverFixture = FunFixture[WebDriver](
    setup = { _ =>
      new FirefoxDriver()
    },
    teardown = { driver =>
      driver.close()
    }
  )

  driverFixture.test("Should read planet list") { driver =>
    driver.get(getClass.getResource("/planet_list_component_reader/planets.html").toURI.toString)
    val playerPlanets = testReadPlanet(driver)
    assertEquals(playerPlanets.size, 10)
    assertEquals(playerPlanets.head, PlayerPlanet(PlanetId("33652802"), Coordinates(3, 130, 11)))
  }

  private def testReadPlanet(driver: WebDriver): List[PlayerPlanet] =
    new PlanetListComponentReader(driver).readPlanetList()
}
