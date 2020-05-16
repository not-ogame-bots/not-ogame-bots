package not.ogame.bots.selenium

import cats.effect.IO
import not.ogame.bots.{Credentials, OgameDriver, PlanetFactories}
import org.openqa.selenium.{By, WebDriver}

import scala.jdk.CollectionConverters._
import cats.implicits._
import WebDriverSyntax._

class SeleniumOgameDriver(credentials: Credentials)(implicit webDriver: WebDriver) extends OgameDriver[IO] {

  override def login(): IO[Unit] = {
    val url = "https://lobby.ogame.gameforge.com/pl_PL/accounts"
    for {
      _                 <- go to url
      loginRegisterTabs <- findMany(By.id("loginRegisterTabs"))
      _ <- if (loginRegisterTabs.nonEmpty) {
        insetCredentials() >> (go to url)
      } else {
        IO.unit
      }
      _ <- selectUniverse()
      _ <- switchToOtherTab()
      _ <- waitForElement(By.className("OGameClock"))
      _ <- IO.delay(webDriver.close())
      _ <- switchToAnyOpenTab()
    } yield ()
  }

  private def insetCredentials(): IO[Unit] =
    IO.delay {
      webDriver.findElement(By.id("loginRegisterTabs")).findElement(By.className("tabsList")).findElement(By.tagName("li")).click()
      webDriver.findElement(By.name("email")).sendKeys(credentials.login)
      webDriver.findElement(By.name("password")).sendKeys(credentials.password)
      Thread.sleep(700)
      webDriver.findElement(By.className("button-lg")).click()
    } >> waitForElement(By.id("joinGame")).void

  private def selectUniverse(): IO[Unit] = waitForElement(By.className("rt-tr")) >> IO.delay {
    webDriver
      .findElements(By.className("rt-tr"))
      .asScala
      .find(_.getText.contains(credentials.universeName))
      .get
      .findElement(By.className("btn-primary"))
      .click()
  }

  override def getFactories(planetId: String): IO[PlanetFactories] = IO.pure(PlanetFactories(1))
}
