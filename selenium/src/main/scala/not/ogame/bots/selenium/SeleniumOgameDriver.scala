package not.ogame.bots.selenium

import java.time.{Clock, Instant}

import cats.effect.{IO, Timer}
import cats.implicits._
import eu.timepit.refined.numeric.NonNegative
import not.ogame.bots._
import not.ogame.bots.selenium.WebDriverSyntax._
import not.ogame.bots.selenium.WebDriverUtils._
import org.openqa.selenium.{By, WebDriver}

import scala.concurrent.duration._

class SeleniumOgameDriver(credentials: Credentials)(implicit webDriver: WebDriver, timer: Timer[IO], clock: Clock) extends OgameDriver[IO] {
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
      _ <- clickF(By.className("button-lg"))
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
      universeText = list
        .find(_.getText.contains(credentials.universeName))
        .getOrElse(throw new IllegalStateException("Couldn't find universeText"))
      universeBtn <- universeText.find(By.className("btn-primary"))
      _ <- universeBtn.clickF()
    } yield ()

  override def readSuppliesPage(planetId: String): IO[SuppliesPageData] =
    for {
      _ <- go to suppliesPageUrl(planetId)
      currentResources <- readCurrentResources
      currentProduction <- readCurrentProduction
      currentCapacity <- readCurrentCapacity
      suppliesLevels <- SuppliesBuilding.values.toList
        .map(suppliesBuilding => suppliesBuilding -> getSuppliesBuildingLevel(suppliesBuilding))
        .traverse {
          case (building, fetchLevel) => fetchLevel.map(level => building -> refineVUnsafe[NonNegative, Int](level))
        }
        .map(list => SuppliesBuildingLevels(list.toMap))
      currentBuildingProgress <- readCurrentBuildingProgress
    } yield SuppliesPageData(
      clock.instant(),
      currentResources,
      currentProduction,
      currentCapacity,
      suppliesLevels,
      currentBuildingProgress
    )

  override def readFacilityBuildingsLevels(planetId: String): IO[FacilitiesBuildingLevels] =
    for {
      _ <- safeUrl(facilitiesPageUrl(planetId))
      facilityLevels <- FacilityBuilding.values.toList
        .map(facilityBuilding => facilityBuilding -> getFacilityBuildingLevel(facilityBuilding))
        .traverse { case (building, fetchLevel) => fetchLevel.map(level => building -> refineVUnsafe[NonNegative, Int](level)) }
        .map(list => FacilitiesBuildingLevels(list.toMap))
    } yield facilityLevels

  private def suppliesPageUrl(planetId: String) = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=supplies&cp=$planetId"
  }

  private def facilitiesPageUrl(planetId: String) = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=facilities&cp=$planetId"
  }

  private def getShipyardUrl(credentials: Credentials, planetId: String): String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=shipyard&cp=$planetId"
  }

  private def readCurrentResources: IO[Resources] =
    for {
      currentMetal <- readInt(By.id("metal_box"))
      currentCrystal <- readInt(By.id("crystal_box"))
      currentDeuterium <- readInt(By.id("deuterium_box"))
      currentEnergy <- readInt(By.id("energy_box"))
    } yield Resources(currentMetal, currentCrystal, currentDeuterium, currentEnergy)

  private def readCurrentProduction: IO[Resources] =
    for {
      metalProduction <- getProduction("metal_box")
      crystalProduction <- getProduction("crystal_box")
      deuteriumProduction <- getProduction("deuterium_box")
    } yield Resources(metalProduction, crystalProduction, deuteriumProduction, 0)

  private def readCurrentCapacity: IO[Resources] =
    for {
      metalProduction <- getCapacity("metal_box")
      crystalProduction <- getCapacity("crystal_box")
      deuteriumProduction <- getCapacity("deuterium_box")
    } yield Resources(metalProduction, crystalProduction, deuteriumProduction, 0)

  private def getProduction(id: String): IO[Int] =
    find(By.id(id))
      .map(_.getAttribute("title"))
      .map(getProductionFromTooltip)

  private def getCapacity(id: String): IO[Int] =
    find(By.id(id))
      .map(_.getAttribute("title"))
      .map(getCapacityFromTooltip)

  private def readCurrentBuildingProgress: IO[Option[BuildingProgress]] =
    for {
      _ <- waitForElement(By.className("construction"))
      buildingCountdown <- findMany(By.id("buildingCountdown")).map(_.headOption)
      seconds = buildingCountdown.map(_.getText).map(timeTextToSeconds)
      buildingProgress = seconds.map(s => BuildingProgress(Instant.now().plusSeconds(s)))
    } yield buildingProgress

  private def timeTextToSeconds(timeText: String): Int =
    timeText.split(" ").map(parseTimeSection).sum

  private def parseTimeSection(text: String): Int = {
    val digits = text.filter(_.isDigit)
    if (digits.isEmpty) {
      0
    } else {
      val number = digits.toInt
      if (text.contains("s")) {
        number
      } else if (text.contains("m")) {
        number * 60
      } else {
        number * 60 * 60
      }
    }
  }

  private def getProductionFromTooltip(text: String): Int = {
    getNumberFromTooltip(text)(2)
  }

  private def getCapacityFromTooltip(text: String): Int = {
    getNumberFromTooltip(text)(1)
  }

  private def getNumberFromTooltip(text: String) = {
    text.linesIterator.toList
      .map(_.trim)
      .filter(_.startsWith("<td>"))
      .map(_.filter(_.isDigit).toInt)
  }

  private def getFacilityBuildingLevel(facilityBuilding: FacilityBuilding): IO[Int] =
    getBuildingLevel(getComponentName(facilityBuilding))

  private def getSuppliesBuildingLevel(suppliesBuilding: SuppliesBuilding): IO[Int] =
    getBuildingLevel(getComponentName(suppliesBuilding))

  private def getBuildingLevel(componentName: String) = {
    for {
      technologies <- waitForElement(By.id("technologies"))
      buildingComponent <- technologies.find(By.className(componentName))
      level <- buildingComponent.find(By.className("level"))
    } yield level.getText.toInt
  }

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

  private def getComponentName(facilityBuilding: FacilityBuilding): String = {
    facilityBuilding match {
      case FacilityBuilding.RoboticsFactory => "roboticsFactory"
      case FacilityBuilding.Shipyard        => "shipyard"
      case FacilityBuilding.ResearchLab     => "researchLaboratory"
      case FacilityBuilding.NaniteFactory   => "naniteFactory"
    }
  }

  override def buildSuppliesBuilding(planetId: String, suppliesBuilding: SuppliesBuilding): IO[Unit] =
    buildBuilding(planetId, getComponentName(suppliesBuilding))

  override def buildFacilityBuilding(planetId: String, facilityBuilding: FacilityBuilding): IO[Unit] =
    buildBuilding(planetId, getComponentName(facilityBuilding))

  private def buildBuilding(planetId: String, componentName: String) = {
    for {
      _ <- go to suppliesPageUrl(planetId)
      technologies <- waitForElement(By.id("technologies"))
      buildingComponent <- technologies.find(By.className(componentName))
      upgrade <- buildingComponent.find(By.className("upgrade"))
      _ <- upgrade.clickF()
    } yield ()
  }

  override def buildShips(planetId: String, shipType: ShipType, count: Int): IO[Unit] = {
    for {
      _ <- safeUrl(getShipyardUrl(credentials, planetId))
      _ <- find(By.id("technologies")).flatMap(_.find(By.className(shipTypeToClassName(shipType)))).flatMap(_.clickF())
      _ <- waitForElement(By.id("build_amount"))
      _ <- find(By.id("build_amount")).flatMap(_.sendKeysF(count.toString))
      _ <- find(By.className("upgrade")).flatMap(_.clickF())
    } yield ()
  }

  private def shipTypeToClassName(shipType: ShipType): String = {
    shipType match {
      case ShipType.SMALL_CARGO_SHIP => "transporterSmall"
      case ShipType.LARGE_CARGO_SHIP => "transporterLarge"
      case ShipType.ESPIONAGE_PROBE  => "espionageProbe"
      case ShipType.CRUISER          => "cruiser"
      case ShipType.EXPLORER         => "explorer"
      case ShipType.LIGHT_FIGHTER    => "fighterLight"
      case ShipType.HEAVY_FIGHTER    => "fighterHeavy"
      case ShipType.BATTLESHIP       => "battleship"
      case ShipType.INTERCEPTOR      => "interceptor"
      case ShipType.DESTROYER        => "destroyer"
      case ShipType.RECYCLER         => "recycler"
      case ShipType.BOMBER           => "bomber"
      case ShipType.REAPER           => "reaper"
    }
  }

  private def getFleetDispatchUrl(credentials: Credentials, planetId: String): String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=fleetdispatch&cp=$planetId"
  }

  def checkFleetOnPlanet(planetId: String, shipType: ShipType): IO[Int] = {
    for {
      _ <- safeUrl(getFleetDispatchUrl(credentials, planetId))
      technologies <- findMany(By.id("technologies"))
      result <- if (technologies.isEmpty) {
        IO.pure(0)
      } else {
        find(By.id("technologies"))
          .flatMap(_.find(By.className(shipTypeToClassName(shipType))))
          .flatMap(_.readInt(By.className("amount")))
      }
    } yield result
  }
}
