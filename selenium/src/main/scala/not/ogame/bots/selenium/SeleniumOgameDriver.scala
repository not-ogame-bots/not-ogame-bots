package not.ogame.bots.selenium

import cats.effect.{IO, Timer}
import cats.implicits._
import not.ogame.bots.selenium.WebDriverSyntax._
import not.ogame.bots.selenium.WebDriverUtils._
import not.ogame.bots.{Credentials, OgameDriver, SuppliesBuilding, SuppliesLevels}
import org.openqa.selenium.{By, WebDriver, WebElement}

import scala.concurrent.duration._

class SeleniumOgameDriver(credentials: Credentials)(implicit webDriver: WebDriver, timer: Timer[IO]) extends OgameDriver[IO] {
  override def login(): IO[Unit] = {
    val universeListUrl = "https://lobby.ogame.gameforge.com/pl_PL/accounts"
    for {
      _ <- go to universeListUrl
      _ <- loginImpl(universeListUrl)
      _ <- selectUniverse()
      _ <- webDriver.closeF()
      _ <- switchToAnyOpenTab()
      _ <- waitForElement(By.className("OGameClock"))
    } yield ()
  }

  private def loginImpl(universeListUrl: String): IO[Unit] = {
    if (webDriver.getCurrentUrl == universeListUrl) {
      IO.unit
    } else {
      insetCredentials() >> (go to universeListUrl)
    }
  }

  private def insetCredentials(): IO[Unit] =
    for {
      _ <- clickLoginTab()
      _ <- find(By.name("email")).flatMap(_.typeText(credentials.login))
      _ <- find(By.name("password")).flatMap(_.typeText(credentials.password))
      _ <- IO.sleep(700 milli)
      _ <- find(By.className("button-lg")).flatMap(_.clickF())
      _ <- waitForElement(By.id("joinGame"))
    } yield ()

  private def clickLoginTab(): IO[Unit] =
    find(By.id("loginRegisterTabs"))
      .flatMap(_.find(By.className("tabsList")))
      .flatMap(_.find(By.tagName("li")))
      .flatMap(_.clickF())

  private def selectUniverse(): IO[Unit] =
    for {
      list <- waitForElements(By.className("rt-tr"))
      universeText <- list
        .find(_.getText.contains(credentials.universeName))
        .map(IO.pure)
        .getOrElse(IO.raiseError[WebElement](new IllegalStateException("Couldn't find universeText")))
      universeBtn <- universeText.find(By.className("btn-primary"))
      _ <- universeBtn.clickF()
    } yield ()

  override def getSuppliesLevels(planetId: String): IO[SuppliesLevels] =
    for {
      _ <- go to s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=supplies&cp=$planetId"
      suppliesLevels <- SuppliesBuilding.values.toList
        .map(suppliesBuilding => suppliesBuilding -> getBuildingLevel(suppliesBuilding))
        .traverse { case (a, b) => b.map(a -> _) }
        .map(list => SuppliesLevels(list.toMap))
    } yield suppliesLevels

  private def getBuildingLevel(suppliesBuilding: SuppliesBuilding): IO[Int] =
    for {
      technologies <- waitForElement(By.id("technologies"))
      buildingComponent <- technologies.find(By.className(getComponentName(suppliesBuilding)))
      level <- buildingComponent.find(By.className("level"))
    } yield level.getText.toInt

  private def getComponentName(suppliesBuilding: SuppliesBuilding): String = {
    suppliesBuilding match {
      case SuppliesBuilding.METAL_MINE => "metalMine"
      case SuppliesBuilding.CRYSTAL_MINE => "crystalMine"
      case SuppliesBuilding.DEUTERIUM_SYNTHESIZER => "deuteriumSynthesizer"
      case SuppliesBuilding.SOLAR_PLANT => "solarPlant"
      case SuppliesBuilding.METAL_STORAGE => "metalStorage"
      case SuppliesBuilding.CRYSTAL_STORAGE => "crystalStorage"
      case SuppliesBuilding.DEUTERIUM_STORAGE => "deuteriumStorage"
    }
  }
}
