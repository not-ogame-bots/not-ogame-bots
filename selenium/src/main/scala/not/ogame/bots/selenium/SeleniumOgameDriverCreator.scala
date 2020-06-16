package not.ogame.bots.selenium

import cats.effect.{Sync, Timer}
import not.ogame.bots.{Credentials, LocalClock, OgameDriver, OgameDriverCreator}
import org.openqa.selenium.WebDriver

class SeleniumOgameDriverCreator[F[_]: Sync](webDriver: WebDriver)(implicit timer: Timer[F], clock: LocalClock)
    extends OgameDriverCreator[F] {
  override def create(credentials: Credentials): OgameDriver[F] = {
    implicit val d = webDriver
    new SeleniumOgameDriver(credentials, new OgameUrlProvider(credentials))
  }
}
