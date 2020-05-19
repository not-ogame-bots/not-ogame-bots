package not.ogame.bots.selenium

import cats.effect.{IO, Timer}
import cats.implicits._
import not.ogame.bots._
import not.ogame.bots.selenium.WebDriverSyntax._
import not.ogame.bots.selenium.WebDriverUtils._
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

  override def readSuppliesPage(planetId: String): IO[SuppliesPageData] =
    for {
      _ <- go to s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=supplies&cp=$planetId"
      currentResources <- readCurrentResources
      suppliesLevels <- SuppliesBuilding.values.toList
        .map(suppliesBuilding => suppliesBuilding -> getBuildingLevel(suppliesBuilding))
        .traverse { case (a, b) => b.map(a -> _) }
        .map(list => SuppliesBuildingLevels(list.toMap))
      suppliesPageData <- IO.pure(SuppliesPageData(currentResources, suppliesLevels))
    } yield suppliesPageData

  private def readCurrentResources: IO[Resources] =
    for {
      currentMetal <- find(By.id("metal_box")).map(_.getText.filter(_.isDigit).toInt)
      currentCrystal <- find(By.id("crystal_box")).map(_.getText.filter(_.isDigit).toInt)
      currentDeuterium <- find(By.id("deuterium_box")).map(_.getText.filter(_.isDigit).toInt)
      currentResources <- IO.pure(Resources(currentMetal, currentCrystal, currentDeuterium))
    } yield currentResources

  private def getBuildingLevel(suppliesBuilding: SuppliesBuilding): IO[Int] =
    for {
      technologies <- waitForElement(By.id("technologies"))
      buildingComponent <- technologies.find(By.className(getComponentName(suppliesBuilding)))
      level <- buildingComponent.find(By.className("level"))
    } yield level.getText.toInt

  private def getComponentName(suppliesBuilding: SuppliesBuilding): String = {
    suppliesBuilding match {
      case SuppliesBuilding.MetalMine            => "metalMine"
      case SuppliesBuilding.CrystalMine          => "crystalMine"
      case SuppliesBuilding.DeuteriumSynthesizer => "deuteriumSynthesizer"
      case SuppliesBuilding.SolarPlant           => "solarPlant"
      case SuppliesBuilding.MetalStorage         => "metalStorage"
      case SuppliesBuilding.CrystalStorage       => "crystalStorage"
      case SuppliesBuilding.DeuteriumStorage     => "deuteriumStorage"
    }
  }

  override def buildSuppliesBuilding(planetId: String, suppliesBuilding: SuppliesBuilding): IO[Unit] =
    for {
      _ <- go to s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=supplies&cp=$planetId"
      technologies <- waitForElement(By.id("technologies"))
      buildingComponent <- technologies.find(By.className(getComponentName(suppliesBuilding)))
      upgrade <- buildingComponent.find(By.className("upgrade"))
      _ <- upgrade.clickF()
    } yield ()
}
