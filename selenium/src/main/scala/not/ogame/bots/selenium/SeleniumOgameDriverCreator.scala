package not.ogame.bots.selenium

import java.time.Clock

import cats.effect.{Concurrent, Resource, Sync, Timer}
import not.ogame.bots.{Credentials, OgameDriver, OgameDriverCreator}
import org.openqa.selenium.firefox.FirefoxDriver

class SeleniumOgameDriverCreator[F[_]: Sync](implicit timer: Timer[F], clock: Clock) extends OgameDriverCreator[F] {
  override def create(credentials: Credentials): Resource[F, OgameDriver[F]] = {
    Resource
      .make(Sync[F].delay(new FirefoxDriver()))(r => Sync[F].delay(r.close()))
      .map { implicit driver =>
        new SeleniumOgameDriver(credentials)
      }
  }
}
