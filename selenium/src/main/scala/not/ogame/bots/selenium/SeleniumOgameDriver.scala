package not.ogame.bots.selenium

import cats.effect.IO
import not.ogame.bots.selenium.WebDriverUtils.WebDriverImprovements
import not.ogame.bots.{Credentials, OgameDriver, PlanetFactories}
import org.openqa.selenium.{By, WebDriver}

import scala.jdk.CollectionConverters._

class SeleniumOgameDriver(private val credentials: Credentials, private val webDriver: WebDriver) extends OgameDriver[IO] {

  override def login(): IO[Unit] =
    IO.delay(() -> {
      val url = "https://lobby.ogame.gameforge.com/pl_PL/accounts"
      webDriver.get(url)
      if (!webDriver.findElements(By.id("loginRegisterTabs")).isEmpty) {
        insetCredentials()
        webDriver.get(url)
      }
      selectUniverse()
      webDriver.switchToOtherTab()
      webDriver.waitForElement(By.className("OGameClock"))
      webDriver.close()
      webDriver.switchToAnyOpenTab()
    })

  private def insetCredentials() {
    webDriver.findElement(By.id("loginRegisterTabs")).findElement(By.className("tabsList")).findElement(By.tagName("li")).click()
    webDriver.findElement(By.name("email")).sendKeys(credentials.login)
    webDriver.findElement(By.name("password")).sendKeys(credentials.password)
    Thread.sleep(700)
    webDriver.findElement(By.className("button-lg")).click()
    webDriver.waitForElement(By.id("joinGame"))
  }

  private def selectUniverse() {
    webDriver.waitForElement(By.className("rt-tr"))
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
