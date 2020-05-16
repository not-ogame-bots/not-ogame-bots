package not.ogame.bots.selenium

import cats.effect.IO
import not.ogame.bots.{Credentials, OgameDriver, OgameDriverCreator}
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

class SeleniumOgameDriverCreator extends OgameDriverCreator[IO] {
  private implicit val driver: WebDriver = new FirefoxDriver()

  override def create(credentials: Credentials): OgameDriver[IO] = new SeleniumOgameDriver(credentials)
}
