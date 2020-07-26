package not.ogame.bots.ordon.core

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import not.ogame.bots.selenium.{OgameUrlProvider, SeleniumOgameDriver, WebDriverResource}
import not.ogame.bots.{Credentials, LocalClock, RealLocalClock}

abstract class OrdonMainLoop extends IOApp {
  val credentials: Credentials
  val actions: List[OrdonAction]

  private implicit val clock: LocalClock = new RealLocalClock()

  override def run(args: List[String]): IO[ExitCode] = {
    if (System.getProperty("webdriver.gecko.driver", "").isEmpty) {
      System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    }
    if (System.getProperty("webdriver.chrome.driver", "").isEmpty) {
      System.setProperty("webdriver.chrome.driver", "selenium/chromedriver-v83")
    }
    runBot()
  }

  private def runBot(): IO[ExitCode] = {
    WebDriverResource
      .firefox[IO]()
      .map(driver => {
        implicit val d = driver
        new SeleniumOgameDriver[IO](credentials, new OgameUrlProvider(credentials))
      })
      .use { ogame =>
        ogame.login() >> runForEver(ogame, actions)
      }
      .as(ExitCode.Success)
      .handleErrorWith(e => {
        e.printStackTrace()
        runBot()
      })
  }

  private def runForEver(ogame: SeleniumOgameDriver[IO], actions: List[OrdonAction]): IO[Unit] = {
    IO.delay({
      new Core(new OrdonOgameDriver(ogame), actions).run()
    })
  }
}
