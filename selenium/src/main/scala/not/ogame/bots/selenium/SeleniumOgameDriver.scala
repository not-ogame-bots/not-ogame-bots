package not.ogame.bots.selenium

import cats.effect.{Sync, Timer}
import cats.implicits._
import eu.timepit.refined.numeric.NonNegative
import not.ogame.bots._
import not.ogame.bots.selenium.EasySelenium._
import not.ogame.bots.selenium.WebDriverUtils._
import org.openqa.selenium.{By, WebDriver}

import scala.concurrent.duration._

class SeleniumOgameDriver[F[_]: Sync](credentials: Credentials)(implicit webDriver: WebDriver, timer: Timer[F], clock: LocalClock)
    extends OgameDriver[F] {
  override def login(): F[Unit] = {
    val universeListUrl = "https://lobby.ogame.gameforge.com/pl_PL/accounts"
    for {
      _ <- webDriver.goto(universeListUrl)
      _ <- loginImpl(universeListUrl)
      _ <- selectUniverse()
      _ <- webDriver.closeF()
      _ <- webDriver.switchToAnyOpenTab()
      _ <- webDriver.waitForElementF(By.className("OGameClock"))
    } yield ()
  }

  private def loginImpl(universeListUrl: String): F[Unit] = {
    webDriver.waitForElement(By.className("gameNav"))
    if (webDriver.getCurrentUrl == universeListUrl) {
      Sync[F].unit
    } else {
      insetCredentials() >> webDriver.goto(universeListUrl)
    }
  }

  private def insetCredentials(): F[Unit] =
    for {
      _ <- clickLoginTab()
      _ <- webDriver.find(By.name("email")).flatMap(_.typeText(credentials.login))
      _ <- webDriver.find(By.name("password")).flatMap(_.typeText(credentials.password))
      _ <- Timer[F].sleep(700 milli)
      _ <- webDriver.waitForElementF(By.className("button-lg"))
      _ <- webDriver.clickF(By.className("button-lg"))
      _ <- webDriver.waitForElementF(By.id("joinGame"))
    } yield ()

  private def clickLoginTab(): F[Unit] =
    webDriver
      .find(By.id("loginRegisterTabs"))
      .flatMap(_.find(By.className("tabsList")))
      .flatMap(_.find(By.tagName("li")))
      .flatMap(_.clickF())

  private def selectUniverse(): F[Unit] =
    for {
      list <- webDriver.waitForElementsF(By.className("rt-tr"))
      universeText = list
        .find(_.getText.contains(credentials.universeName))
        .getOrElse(throw new IllegalStateException("Couldn't find universeText"))
      universeBtn <- universeText.find(By.className("btn-primary"))
      _ <- universeBtn.clickF()
    } yield ()

  override def readSuppliesPage(planetId: PlanetId): F[SuppliesPageData] =
    for {
      _ <- webDriver.goto(suppliesPageUrl(planetId))
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
      currentShipyardProgress <- readCurrentShipyardProgress
    } yield SuppliesPageData(
      clock.now(),
      currentResources,
      currentProduction,
      currentCapacity,
      suppliesLevels,
      currentBuildingProgress,
      currentShipyardProgress
    )

  override def readFacilityPage(planetId: PlanetId): F[FacilityPageData] =
    for {
      _ <- webDriver.safeUrlF(facilitiesPageUrl(planetId))
      currentResources <- readCurrentResources
      currentProduction <- readCurrentProduction
      currentCapacity <- readCurrentCapacity
      facilityLevels <- FacilityBuilding.values.toList
        .map(facilityBuilding => facilityBuilding -> getFacilityBuildingLevel(facilityBuilding))
        .traverse { case (building, fetchLevel) => fetchLevel.map(level => building -> refineVUnsafe[NonNegative, Int](level)) }
        .map(list => FacilitiesBuildingLevels(list.toMap))
      currentBuildingProgress <- readCurrentBuildingProgress
    } yield FacilityPageData(clock.now(), currentResources, currentProduction, currentCapacity, facilityLevels, currentBuildingProgress)

  private def suppliesPageUrl(planetId: PlanetId) = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=supplies&cp=$planetId"
  }

  private def facilitiesPageUrl(planetId: PlanetId) = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=facilities&cp=$planetId"
  }

  private def getShipyardUrl(credentials: Credentials, planetId: PlanetId): String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=shipyard&cp=$planetId"
  }

  private def readCurrentResources: F[Resources] =
    for {
      currentMetal <- webDriver.readInt(By.id("metal_box"))
      currentCrystal <- webDriver.readInt(By.id("crystal_box"))
      currentDeuterium <- webDriver.readInt(By.id("deuterium_box"))
      currentEnergy <- webDriver.readInt(By.id("energy_box"))
    } yield Resources(currentMetal, currentCrystal, currentDeuterium, currentEnergy)

  private def readCurrentProduction: F[Resources] =
    for {
      metalProduction <- getProduction("metal_box")
      crystalProduction <- getProduction("crystal_box")
      deuteriumProduction <- getProduction("deuterium_box")
    } yield Resources(metalProduction, crystalProduction, deuteriumProduction)

  private def readCurrentCapacity: F[Resources] =
    for {
      metalProduction <- getCapacity("metal_box")
      crystalProduction <- getCapacity("crystal_box")
      deuteriumProduction <- getCapacity("deuterium_box")
    } yield Resources(metalProduction, crystalProduction, deuteriumProduction)

  private def getProduction(id: String): F[Int] =
    webDriver
      .find(By.id(id))
      .map(_.getAttribute("title"))
      .map(getProductionFromTooltip)

  private def getCapacity(id: String): F[Int] =
    webDriver
      .find(By.id(id))
      .map(_.getAttribute("title"))
      .map(getCapacityFromTooltip)

  private def readCurrentBuildingProgress: F[Option[BuildingProgress]] =
    for {
      _ <- webDriver.waitForElementF(By.className("construction"))
      buildingCountdown <- webDriver.findMany(By.id("buildingCountdown")).map(_.headOption)
      seconds = buildingCountdown.map(_.getText).map(timeTextToSeconds)
      buildingProgress = seconds.map(s => BuildingProgress(clock.now().plusSeconds(s)))
    } yield buildingProgress

  private def readCurrentShipyardProgress: F[Option[BuildingProgress]] =
    for {
      _ <- webDriver.waitForElementF(By.className("construction"))
      shipyardCountdown <- webDriver.findMany(By.id("shipyardCountdown")).map(_.headOption)
      seconds = shipyardCountdown.map(_.getText).map(timeTextToSeconds)
      buildingProgress = seconds.map(s => BuildingProgress(clock.now().plusSeconds(s)))
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

  private def getFacilityBuildingLevel(facilityBuilding: FacilityBuilding): F[Int] =
    getBuildingLevel(getComponentName(facilityBuilding))

  private def getSuppliesBuildingLevel(suppliesBuilding: SuppliesBuilding): F[Int] =
    getBuildingLevel(getComponentName(suppliesBuilding))

  private def getBuildingLevel(componentName: String) = {
    for {
      technologies <- webDriver.waitForElementF(By.id("technologies"))
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

  override def buildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding): F[Unit] = {
    webDriver.goto(suppliesPageUrl(planetId)) >>
      buildBuilding(planetId, getComponentName(suppliesBuilding))
  }

  override def buildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding): F[Unit] = {
    webDriver.goto(facilitiesPageUrl(planetId)) >>
      buildBuilding(planetId, getComponentName(facilityBuilding))
  }

  private def buildBuilding(planetId: PlanetId, componentName: String) = {
    for {
      technologies <- webDriver.waitForElementF(By.id("technologies"))
      buildingComponent <- technologies.find(By.className(componentName))
      upgrade <- buildingComponent.find(By.className("upgrade"))
      _ <- upgrade.clickF()
    } yield ()
  }
  override def buildShips(planetId: PlanetId, shipType: ShipType, count: Int): F[Unit] = {
    for {
      _ <- webDriver.safeUrlF(getShipyardUrl(credentials, planetId))
      _ <- webDriver.find(By.id("technologies")).flatMap(_.find(By.className(shipTypeToClassName(shipType)))).flatMap(_.clickF())
      _ <- webDriver.waitForElementF(By.id("build_amount"))
      _ <- webDriver.find(By.id("build_amount")).flatMap(_.sendKeysF(count.toString))
      _ <- webDriver.find(By.className("upgrade")).flatMap(_.clickF())
    } yield ()
  }

  private def shipTypeToClassName(shipType: ShipType): String = {
    shipType match {
      case ShipType.LightFighter   => "fighterLight"
      case ShipType.HeavyFighter   => "fighterHeavy"
      case ShipType.Cruiser        => "cruiser"
      case ShipType.Battleship     => "battleship"
      case ShipType.Interceptor    => "interceptor"
      case ShipType.Bomber         => "bomber"
      case ShipType.Destroyer      => "destroyer"
      case ShipType.DeathStar      => "deathstar"
      case ShipType.Reaper         => "reaper"
      case ShipType.Explorer       => "explorer"
      case ShipType.SmallCargoShip => "transporterSmall"
      case ShipType.LargeCargoShip => "transporterLarge"
      case ShipType.ColonyShip     => "colonyShip"
      case ShipType.Recycler       => "recycler"
      case ShipType.EspionageProbe => "espionageProbe"
    }
  }

  private def getFleetDispatchUrl(credentials: Credentials, planetId: PlanetId): String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=fleetdispatch&cp=$planetId"
  }

  override def readFleetPage(planetId: PlanetId): F[FleetPageData] =
    for {
      _ <- webDriver.goto(getFleetDispatchUrl(credentials, planetId))
      currentResources <- readCurrentResources
      currentProduction <- readCurrentProduction
      currentCapacity <- readCurrentCapacity
      ships <- readShips()
    } yield FleetPageData(
      clock.now(),
      currentResources,
      currentProduction,
      currentCapacity,
      ships
    )

  def checkFleetOnPlanet(planetId: PlanetId): F[Map[ShipType, Int]] = {
    for {
      _ <- webDriver.safeUrlF(getFleetDispatchUrl(credentials, planetId))
      result <- readShips()
    } yield result
  }

  private def readShips(): F[Map[ShipType, Int]] = {
    for {
      technologies <- webDriver.findMany(By.id("technologies"))
      result <- if (technologies.isEmpty) {
        Sync[F].pure(ShipType.values.map(_ -> 0).toMap)
      } else {
        ShipType.values
          .map { ship =>
            webDriver
              .find(By.id("technologies"))
              .flatMap(_.find(By.className(shipTypeToClassName(ship))))
              .flatMap(_.readInt(By.className("amount")))
              .map(ship -> _)
          }
          .toList
          .sequence
          .map(_.toMap)
      }
    } yield result
  }

  def readAllFleets(): F[List[Fleet]] = {
    Sync[F].delay({
      webDriver.safeUrl(
        s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=componentOnly&component=eventList&ajax=1"
      )
      new AllFleetsComponentReader(webDriver).readAllFleets()
    })
  }

  override def readMyFleets(): F[List[MyFleet]] = {
    Sync[F].delay({
      webDriver.safeUrl(
        s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=movement"
      )
      new MyFleetsComponentReader(webDriver).readMyFleets()
    })
  }

  override def sendFleet(sendFleetRequest: SendFleetRequest): F[Unit] = {
    Sync[F].delay(new SendFleetAction(webDriver, credentials).sendFleet(sendFleetRequest))
  }

  override def returnFleet(fleetId: FleetId): F[Unit] = {
    Sync[F].delay {
      webDriver.get(
        s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=movement&return=${fleetId.filter(_.isDigit)}"
      )
    }
  }

  override def readPlanets(): F[List[PlayerPlanet]] = {
    Sync[F].delay {
      webDriver.get(s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=overview")
      new PlanetListComponentReader(webDriver).readPlanetList()
    }
  }

  override def checkIsLoggedIn(): F[Boolean] = {
    webDriver
      .waitForElementF(By.className("OGameClock"))
      .map(_ => true)
      .handleError(_ => false)
  }
}
