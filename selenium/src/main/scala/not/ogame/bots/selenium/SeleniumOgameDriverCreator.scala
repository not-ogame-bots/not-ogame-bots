package not.ogame.bots.selenium

import cats.effect.{Sync, Timer}
import not.ogame.bots.{Credentials, LocalClock, OgameDriver}
import org.openqa.selenium.WebDriver

object SeleniumOgameDriverCreator {
  def create[F[_]: Sync: Timer](webDriver: WebDriver, credentials: Credentials)(implicit clock: LocalClock): OgameDriver[F] = {
    implicit val d = webDriver
    new SeleniumOgameDriver(credentials, new OgameUrlProvider(credentials))
  }
}
