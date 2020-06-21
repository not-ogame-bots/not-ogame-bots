package not.ogame.bots.selenium

import cats.Monad
import cats.effect.{Sync, Timer}
import cats.implicits._
import eu.timepit.refined.numeric.NonNegative
import not.ogame.bots._
import not.ogame.bots.selenium.EasySelenium._
import not.ogame.bots.selenium.WebDriverUtils._
import org.openqa.selenium.{By, WebDriver, WebElement}

import scala.concurrent.duration._

class SeleniumOgameDriver[F[_]: Sync](credentials: Credentials, urlProvider: UrlProvider)(
    implicit webDriver: WebDriver,
    timer: Timer[F],
    clock: LocalClock
) extends OgameDriver[F] {
  override def login(): F[Unit] = {
    for {
      _ <- webDriver.goto(urlProvider.universeListUrl)
      _ <- loginImpl(urlProvider.universeListUrl)
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
      _ <- webDriver.goto(urlProvider.suppliesPageUrl(planetId))
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
      _ <- webDriver.safeUrlF(urlProvider.facilitiesPageUrl(planetId))
      currentResources <- readCurrentResources
      currentProduction <- readCurrentProduction
      currentCapacity <- readCurrentCapacity
      facilityLevels <- FacilityBuilding.values.toList
        .map(facilityBuilding => facilityBuilding -> getFacilityBuildingLevel(facilityBuilding))
        .traverse { case (building, fetchLevel) => fetchLevel.map(level => building -> refineVUnsafe[NonNegative, Int](level)) }
        .map(list => FacilitiesBuildingLevels(list.toMap))
      currentBuildingProgress <- readCurrentBuildingProgress
    } yield FacilityPageData(clock.now(), currentResources, currentProduction, currentCapacity, facilityLevels, currentBuildingProgress)

  override def readTechnologyPage(planetId: PlanetId): F[TechnologyPageData] =
    for {
      _ <- webDriver.safeUrlF(urlProvider.getTechnologyUrl(planetId))
      currentResources <- readCurrentResources
      currentProduction <- readCurrentProduction
      currentCapacity <- readCurrentCapacity
      technologyLevels <- Technology.values.toList
        .map(technology => technology -> getTechnologyLevel(technology))
        .traverse { case (building, fetchLevel) => fetchLevel.map(level => building -> refineVUnsafe[NonNegative, Int](level)) }
        .map(list => TechnologyLevels(list.toMap))
      currentReserchProgress <- readCurrentResearchProgress
    } yield TechnologyPageData(clock.now(), currentResources, currentProduction, currentCapacity, technologyLevels, currentReserchProgress)

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
      constructions <- webDriver.waitForElementsF(By.className("construction"))
      buildingProgress <- getConstructionInProgress(constructions, "buildingCountdown")
    } yield buildingProgress

  private def readCurrentShipyardProgress: F[Option[BuildingProgress]] =
    for {
      constructions <- webDriver.waitForElementsF(By.className("construction"))
      buildingProgress <- getConstructionInProgress(constructions, "shipyardCountdown")
    } yield buildingProgress

  private def readCurrentResearchProgress: F[Option[BuildingProgress]] =
    for {
      constructions <- webDriver.waitForElementsF(By.className("construction"))
      buildingProgress <- getConstructionInProgress(constructions, "researchCountdown")
    } yield buildingProgress

  private def getConstructionInProgress(constructions: List[WebElement], countdownId: String): F[Option[BuildingProgress]] = {
    fs2.Stream
      .emits(constructions)
      .evalMap(cElement => cElement.findMany(By.id(countdownId)).map(cElement -> _))
      .collectFirst { case (constructionElement, countdownElement :: _) => constructionElement -> countdownElement }
      .evalMap {
        case (constructionElement, countdownElement) =>
          Monad[F].map2(extractFinishTime(countdownElement), extractConstructionName(constructionElement))(BuildingProgress)
      }
      .compile
      .last
  }

  private def extractFinishTime(countdownElement: WebElement) = {
    countdownElement.readText.map(str => clock.now().plusSeconds(timeTextToSeconds(str)))
  }

  private def extractConstructionName(constructionElement: WebElement) = {
    constructionElement
      .find(By.tagName("tbody"))
      .flatMap(_.find(By.tagName("tr")))
      .flatMap(_.find(By.tagName("th")))
      .map(_.getText)
  }

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
    getLevel(getComponentName(facilityBuilding))

  private def getSuppliesBuildingLevel(suppliesBuilding: SuppliesBuilding): F[Int] =
    getLevel(getComponentName(suppliesBuilding))

  private def getTechnologyLevel(technology: Technology): F[Int] =
    getLevel(getComponentName(technology))

  private def getLevel(componentName: String) = {
    for {
      technologies <- webDriver.waitForElementF(By.id("technologies"))
      buildingComponent <- technologies.find(By.className(componentName))
      level <- buildingComponent.find(By.className("level"))
    } yield level.getText.takeWhile(_.isDigit).toInt //hack for spy boost - X(+2)
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

  private def getComponentName(technology: Technology): String = {
    (technology match {
      case Technology.Energy          => "energy"
      case Technology.Laser           => "laser"
      case Technology.Ion             => "ion"
      case Technology.Hyperspace      => "hyperspace"
      case Technology.Plasma          => "plasma"
      case Technology.CombustionDrive => "combustionDrive"
      case Technology.ImpulseDrive    => "impulseDrive"
      case Technology.HyperspaceDrive => "hyperspaceDrive"
      case Technology.Espionage       => "espionage"
      case Technology.Computer        => "computer"
      case Technology.Astrophysics    => "astrophysics"
      case Technology.ResearchNetwork => "researchNetwork"
      case Technology.Graviton        => "graviton"
      case Technology.Weapons         => "weapons"
      case Technology.Shielding       => "shielding"
      case Technology.Armor           => "armor"
    }) + "Technology"
  }

  override def buildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding): F[Unit] = {
    webDriver.goto(urlProvider.suppliesPageUrl(planetId)) >>
      upgrade(getComponentName(suppliesBuilding))
  }

  override def buildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding): F[Unit] = {
    webDriver.goto(urlProvider.facilitiesPageUrl(planetId)) >>
      upgrade(getComponentName(facilityBuilding))
  }

  override def startResearch(planetId: PlanetId, technology: Technology): F[Unit] = {
    webDriver.goto(urlProvider.getTechnologyUrl(planetId)) >>
      upgrade(getComponentName(technology))
  }

  private def upgrade(componentName: String) = {
    for {
      technologies <- webDriver.waitForElementF(By.id("technologies"))
      buildingComponent <- technologies.find(By.className(componentName))
      upgrade <- buildingComponent.find(By.className("upgrade"))
      _ <- upgrade.clickF()
    } yield ()
  }

  override def buildShips(planetId: PlanetId, shipType: ShipType, count: Int): F[Unit] = {
    for {
      _ <- webDriver.safeUrlF(urlProvider.getShipyardUrl(planetId))
      _ <- webDriver.find(By.id("technologies")).flatMap(_.find(By.className(shipTypeToClassName(shipType)))).flatMap(_.clickF())
      _ <- webDriver.waitForElementF(By.id("build_amount"))
      _ <- webDriver.find(By.id("build_amount")).flatMap(_.sendKeysF(count.toString))
      _ <- webDriver.find(By.className("upgrade")).flatMap(_.clickF())
    } yield ()
  }

  override def buildSolarSatellite(planetId: PlanetId): F[Unit] = {
    for {
      _ <- webDriver.safeUrlF(urlProvider.getShipyardUrl(planetId))
      _ <- webDriver.find(By.id("technologies")).flatMap(_.find(By.className("solarSatellite"))).flatMap(_.clickF())
      _ <- webDriver.waitForElementF(By.id("build_amount"))
      _ <- webDriver.find(By.id("build_amount")).flatMap(_.sendKeysF("1"))
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

  override def readFleetPage(planetId: PlanetId): F[FleetPageData] =
    for {
      _ <- webDriver.goto(urlProvider.getFleetDispatchUrl(planetId))
      currentResources <- readCurrentResources
      currentProduction <- readCurrentProduction
      currentCapacity <- readCurrentCapacity
      fleetSlots = new FleetDispatchComponentReader(webDriver).readSlots()
      ships <- readShips()
    } yield FleetPageData(
      clock.now(),
      currentResources,
      currentProduction,
      currentCapacity,
      fleetSlots,
      ships
    )

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
        urlProvider.readAllFleetsUrl
      )
      new AllFleetsComponentReader(webDriver).readAllFleets()
    })
  }

  override def readMyFleets(): F[MyFleetPageData] = {
    Sync[F].delay({
      webDriver.get(urlProvider.readMyFleetsUrl)
      if (webDriver.getCurrentUrl.contains("movement")) {
        new MyFleetsComponentReader(webDriver).readMyFleets()
      } else if (webDriver.getCurrentUrl.contains("fleetdispatch")) {
        //When there are no fleets movement page redirects to fleetdispatch
        val slots = new FleetDispatchComponentReader(webDriver).readSlots()
        MyFleetPageData(List.empty, MyFleetSlots(slots.currentFleets, slots.maxFleets, slots.currentExpeditions, slots.maxExpeditions))
      } else {
        throw new RuntimeException(s"Couldn't proceed to movement page")
      }
    })
  }

  override def sendFleet(sendFleetRequest: SendFleetRequest): F[Unit] = {
    Sync[F].delay {
      webDriver.safeUrl(urlProvider.getFleetDispatchUrl(sendFleetRequest.from.id))
      new SendFleetAction(webDriver).sendFleet(sendFleetRequest)
    }
  }

  override def returnFleet(fleetId: FleetId): F[Unit] = {
    Sync[F].delay(webDriver.get(urlProvider.returnFleetUrl(fleetId)))
  }

  override def readPlanets(): F[List[PlayerPlanet]] = {
    Sync[F].delay {
      webDriver.get(urlProvider.planetsUrl)
      new PlanetListComponentReader(webDriver).readPlanetList()
    }
  }

  override def checkIsLoggedIn(): F[Boolean] = {
    webDriver
      .waitForElementF(By.className("OGameClock"))
      .map(_ => true)
      .handleError(_ => false)
  }

  override def readMyOffers(): F[List[MyOffer]] = {
    Sync[F].delay {
      webDriver.get(s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=marketplace&tab=overview")
      new MyOffersComponentReader(webDriver).readMyOffers()
    }
  }

  override def createOffer(planetId: PlanetId, newOffer: MyOffer): F[Unit] = {
    Sync[F].delay {
      webDriver.get(
        s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=marketplace&tab=create_offer&cp=$planetId"
      )
      new CreateOfferComponentFiller(webDriver).createOffer(newOffer)
    }
  }
}
