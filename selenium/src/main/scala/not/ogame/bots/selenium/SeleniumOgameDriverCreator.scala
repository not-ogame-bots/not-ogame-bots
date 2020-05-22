package not.ogame.bots.selenium

import java.time.Clock

import cats.effect.{IO, Resource, Timer}
import not.ogame.bots.{Credentials, OgameDriver, OgameDriverCreator}
import org.openqa.selenium.firefox.FirefoxDriver

class SeleniumOgameDriverCreator(implicit timer: Timer[IO], clock: Clock) extends OgameDriverCreator[IO] {
  override def create(credentials: Credentials): Resource[IO, OgameDriver[IO]] = {
    Resource
      .make(IO.delay(new FirefoxDriver()))(r => IO.delay(r.close()))
      .map { implicit driver =>
        new SeleniumOgameDriver(credentials)
      }
  }
}
