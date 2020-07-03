package not.ogame.bots.selenium

import not.ogame.bots.CoordinatesType.{Debris, Moon}
import not.ogame.bots.PlayerActivity.{LessThan15MinutesAgo, MinutesAgo, NotActive}
import not.ogame.bots._
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

  driverFixture.test("Should read player activity") { driver =>
    driver.get(getClass.getResource("/galaxy_component_reader/galaxy.html").toURI.toString)
    val galaxyPage = testReadGalaxy(driver)
    assertEquals(galaxyPage.playerActivityMap(Coordinates(3, 133, 4)), LessThan15MinutesAgo)
    assertEquals(galaxyPage.playerActivityMap(Coordinates(3, 133, 4, Moon)), NotActive)
    assertEquals(galaxyPage.playerActivityMap(Coordinates(3, 133, 5)), MinutesAgo(47))
    assertEquals(galaxyPage.playerActivityMap.contains(Coordinates(3, 133, 2)), false)
    assertEquals(galaxyPage.playerActivityMap.contains(Coordinates(3, 133, 12, Moon)), false)
  }

  driverFixture.test("Should read debris") { driver =>
    driver.get(getClass.getResource("/galaxy_component_reader/galaxy_debris.html").toURI.toString)
    val galaxyPage = testReadGalaxy(driver)
    // TODO: Try parsing debris resources and uncomment
    // assertEquals(galaxyPage.debrisMap(Coordinates(1, 148, 5, Debris)), Resources(51_100, 31_500, 0))
    assertEquals(galaxyPage.debrisMap.contains(Coordinates(1, 148, 5, Debris)), true)
    assertEquals(galaxyPage.debrisMap.contains(Coordinates(1, 148, 6, Debris)), false)
  }

  driverFixture.test("Should read expedition debris") { driver =>
    driver.get(getClass.getResource("/galaxy_component_reader/expedition_debris.html").toURI.toString)
    val galaxyPage = testReadGalaxy(driver)
    // TODO: Try parsing debris resources and uncomment
    // assertEquals(galaxyPage.debrisMap(Coordinates(1, 154, 16, Debris)), Resources(908_000, 722_000, 0))
    assertEquals(galaxyPage.debrisMap.contains(Coordinates(1, 154, 16, Debris)), true)
  }

  driverFixture.test("Should read galaxy without debris on position 16") { driver =>
    driver.get(getClass.getResource("/galaxy_component_reader/expedition_no_debris.html").toURI.toString)
    val galaxyPage = testReadGalaxy(driver)
    assertEquals(galaxyPage.debrisMap.contains(Coordinates(1, 154, 16, Debris)), false)
  }

  private def testReadGalaxy(driver: WebDriver): GalaxyPageData =
    new GalaxyComponentReader(driver).readGalaxyPage()
}
