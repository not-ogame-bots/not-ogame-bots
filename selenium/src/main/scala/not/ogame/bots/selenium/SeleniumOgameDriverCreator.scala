package not.ogame.bots.selenium

import cats.effect.{Resource, Sync, Timer}
import not.ogame.bots.{Credentials, LocalClock, OgameDriver, OgameDriverCreator}
import org.openqa.selenium.MutableCapabilities
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

class SeleniumOgameDriverCreator[F[_]: Sync](options: FirefoxOptions = new FirefoxOptions())(
    implicit timer: Timer[F],
    clock: LocalClock
) extends OgameDriverCreator[F] {
  override def create(credentials: Credentials): Resource[F, OgameDriver[F]] = {
    Resource
      .make(Sync[F].delay(new FirefoxDriver(options)))(r => Sync[F].delay(r.close()))
      .map { implicit driver =>
        new SeleniumOgameDriver(credentials)
      }
  }
}
