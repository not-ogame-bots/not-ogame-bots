package not.ogame.bots.selenium

import cats.effect.{ContextShift, IO, Resource}
import not.ogame.bots._
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

import scala.concurrent.ExecutionContext

class TechnologyPageDataSpec extends CatsEffectSuite with CatsEffectFunFixtures with GecoDriver {
  implicit val clock: LocalClock = new RealLocalClock()
  private val driverFixture = CatsEffectFixture.fromResource(
    Resource
      .make[IO, FirefoxDriver](IO.delay(new FirefoxDriver(new FirefoxOptions().setHeadless(true))))(r => IO.delay(r.close()))
      .map { implicit driver =>
        new SeleniumOgameDriver[IO](Credentials("", "", "", ""), new UrlProvider {
          override def universeListUrl: String = ???
          override def suppliesPageUrl(planetId: String): String = ???
          override def facilitiesPageUrl(planetId: String): String = ???
          override def getShipyardUrl(planetId: String): String = ???
          override def getTechnologyUrl(planetId: String): String =
            getClass.getResource("/technology_page_data/technology_page.html").toURI.toString.replace("file:/", "file:///")
          override def readMyFleetsUrl: String = ???
          override def readAllFleetsUrl: String = ???
          override def getFleetDispatchUrl(planetId: PlanetId): String = ???
          override def returnFleetUrl(fleetId: FleetId): String = ???
          override def planetsUrl: String = ???
        })
      }
  )

  driverFixture.test("Should read planet list") { driver =>
    driver.readTechnologyPage(PlanetId("33653280")).map { page =>
      assertEquals(
        page.technologyIntLevels.values,
        Map[Technology, Int](
          Technology.Armor -> 12,
          Technology.CombustionDrive -> 11,
          Technology.Plasma -> 0,
          Technology.Shielding -> 9,
          Technology.Espionage -> 11,
          Technology.ImpulseDrive -> 5,
          Technology.Weapons -> 11,
          Technology.ResearchNetwork -> 2,
          Technology.Hyperspace -> 8,
          Technology.Computer -> 10,
          Technology.Energy -> 6,
          Technology.Graviton -> 0,
          Technology.Ion -> 3,
          Technology.Laser -> 12,
          Technology.HyperspaceDrive -> 6,
          Technology.Astrophysics -> 9
        )
      )
    }
  }

  implicit lazy val ec: ExecutionContext = ExecutionContext.global
  implicit lazy val cs: ContextShift[IO] = IO.contextShift(ec)
}
