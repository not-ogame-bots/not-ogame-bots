package not.ogame.bots.ordon.core

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import not.ogame.bots._
import not.ogame.bots.ordon.OrdonQuasarConfig
import not.ogame.bots.selenium.{OgameUrlProvider, SeleniumOgameDriver, WebDriverResource}

object Boot extends IOApp {
  private implicit val clock: LocalClock = new RealLocalClock()

  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    System.setProperty("webdriver.chrome.driver", "selenium/chromedriver-v83")
    runBot()
  }

  private def runBot(): IO[ExitCode] = {
    val credentials = OrdonQuasarConfig.getCredentials
    WebDriverResource
      .firefox[IO]()
      .map(driver => {
        implicit val d = driver
        new SeleniumOgameDriver[IO](credentials, new OgameUrlProvider(credentials))
      })
      .use { ogame =>
        ogame.login() >> runForEver(ogame)
      }
      .as(ExitCode.Success)
      .handleErrorWith(e => {
        e.printStackTrace()
        runBot()
      })
  }

  private def runForEver(ogame: SeleniumOgameDriver[IO]): IO[Unit] = {
    IO.delay({
      new Core(new OrdonOgameDriver(ogame), OrdonQuasarConfig.getInitialActionsV2()).run()
    })
  }
}
