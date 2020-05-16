package not.ogame.bots.selenium

import cats.data.OptionT
import cats.effect.{IO, Timer}
import cats.implicits._
import not.ogame.bots.selenium.WebDriverSyntax._
import not.ogame.bots.selenium.WebDriverUtils._
import not.ogame.bots.{Credentials, OgameDriver, PlanetFactories}
import org.openqa.selenium.{By, WebDriver, WebElement}

import scala.concurrent.duration._

class SeleniumOgameDriver(credentials: Credentials)(implicit webDriver: WebDriver, timer: Timer[IO]) extends OgameDriver[IO] {

  override def login(): IO[Unit] = {
    val url = "https://lobby.ogame.gameforge.com/pl_PL/accounts"
    for {
      _                 <- go to url
      loginRegisterTabs <- findMany(By.id("loginRegisterTabs"))
      _                 <- loginImpl(url, loginRegisterTabs)
      _                 <- selectUniverse()
      _                 <- switchToOtherTab()
      _                 <- waitForElement(By.className("OGameClock"))
      _                 <- IO.delay(webDriver.close())
      _                 <- switchToAnyOpenTab()
    } yield ()
  }

  private def loginImpl(url: String, loginRegisterTabs: List[WebElement]): IO[Unit] = {
    if (loginRegisterTabs.nonEmpty) {
      insetCredentials() >> (go to url)
    } else {
      IO.unit
    }
  }

  private def insetCredentials(): IO[Unit] =
    for {
      loginTab    <- findLoginTab()
      _           <- loginTab.clickF()
      email       <- find(By.name("email"))
      _           <- email.typeText(credentials.login)
      password    <- find(By.name("password"))
      _           <- password.typeText(credentials.password)
      _           <- IO.sleep(700 milli)
      loginButton <- find(By.className("button-lg"))
      _           <- loginButton.clickF()
      _           <- waitForElement(By.id("joinGame"))
    } yield ()

  private def findLoginTab(): IO[WebElement] =
    find(By.id("loginRegisterTabs"))
      .flatMap(_.find(By.className("tabsList")))
      .flatMap(_.find(By.tagName("li")))

  private def selectUniverse(): IO[Unit] =
    (for {
      _            <- OptionT.liftF(waitForElement(By.className("rt-tr")))
      list         <- OptionT.liftF(findMany(By.className("rt-tr")))
      universeText <- OptionT.fromOption[IO](list.find(_.getText.contains(credentials.universeName)))
      universeBtn  <- OptionT.liftF(universeText.find(By.className("btn-primary")))
      _            <- OptionT.liftF(universeBtn.clickF())
    } yield ()).value.void

  override def getFactories(planetId: String): IO[PlanetFactories] = IO.pure(PlanetFactories(1))
}
