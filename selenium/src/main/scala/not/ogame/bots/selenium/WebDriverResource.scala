package not.ogame.bots.selenium

import cats.effect.{Resource, Sync}
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

object WebDriverResource {
  def firefox[F[_]: Sync](options: FirefoxOptions = new FirefoxOptions()): Resource[F, WebDriver] =
    Resource.make(Sync[F].delay(new FirefoxDriver(options)))(r => Sync[F].delay(r.close()))

  def chrome[F[_]: Sync](options: ChromeOptions): Resource[F, WebDriver] =
    Resource.make(Sync[F].delay(new ChromeDriver(options)))(r => Sync[F].delay(r.close()))
}
