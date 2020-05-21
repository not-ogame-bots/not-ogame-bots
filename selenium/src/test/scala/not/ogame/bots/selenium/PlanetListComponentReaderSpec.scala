package not.ogame.bots.selenium

import cats.effect.{Clock, IO, Resource, Sync, Timer}
import not.ogame.bots.{Coordinates, PlayerPlanet}
import org.openqa.selenium.firefox.FirefoxDriver

import scala.concurrent.duration.FiniteDuration

class PlanetListComponentReaderSpec extends munit.FunSuite {
  test("Should read planet list") {
    val playerPlanets = runWithWebDriver("/planet_list_component_reader/planets.html", testReadPlanet)
    assertEquals(playerPlanets.size, 7)
    assertEquals(playerPlanets.head, PlayerPlanet("33652802", Coordinates(3, 130, 11)))
  }

  private def testReadPlanet(timer: MyTimer, driver: FirefoxDriver): WaitAndProcess[List[PlayerPlanet]] =
    new PlanetListComponentReader()(driver, timer).readPlanetList()

  private def runWithWebDriver[T](resourcesPage: String, function: (MyTimer, FirefoxDriver) => WaitAndProcess[T]): T = {
    val timer = new MyTimer()
    Resource
      .make(IO.delay(new FirefoxDriver()))(r => IO.delay(r.close()))
      .use(
        driver =>
          for {
            _ <- IO.delay(driver.get(getClass.getResource(resourcesPage).toURI.toString))
            waitAndProcess = function(timer, driver)
            _ <- waitAndProcess.await
            t <- waitAndProcess.process
          } yield t
      )
      .unsafeRunSync()
  }

  class MyTimer extends Timer[IO] {
    override def clock: Clock[IO] = Clock.create(Sync[IO])

    override def sleep(duration: FiniteDuration): IO[Unit] = IO.delay(Thread.sleep(duration.toMillis))
  }
}
