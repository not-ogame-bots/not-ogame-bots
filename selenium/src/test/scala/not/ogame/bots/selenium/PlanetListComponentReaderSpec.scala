package not.ogame.bots.selenium

import cats.effect.{IO, Resource, Timer}
import not.ogame.bots.{Coordinates, PlayerPlanet}
import org.openqa.selenium.firefox.FirefoxDriver

import scala.concurrent.ExecutionContext.Implicits.global

class PlanetListComponentReaderSpec extends munit.FunSuite {
  test("Should read planet list") {
    val playerPlanets = runWithWebDriver("/planet_list_component_reader/planets.html", testReadPlanet)
    assertEquals(playerPlanets.size, 7)
    assertEquals(playerPlanets.head, PlayerPlanet("33652802", Coordinates(3, 130, 11)))
  }

  private def testReadPlanet(timer: Timer[IO], driver: FirefoxDriver): IO[List[PlayerPlanet]] =
    new PlanetListComponentReader()(driver, timer).readPlanetList()

  private def runWithWebDriver[T](resourcesPage: String, function: (Timer[IO], FirefoxDriver) => IO[T]): T = {
    val timer = IO.timer(global)
    Resource
      .make(IO.delay(new FirefoxDriver()))(r => IO.delay(r.close()))
      .use(
        driver =>
          for {
            _ <- IO.delay(driver.get(getClass.getResource(resourcesPage).toURI.toString))
            t <- function(timer, driver)
          } yield t
      )
      .unsafeRunSync()
  }
}
