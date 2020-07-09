package not.ogame.bots.selenium

import not.ogame.bots.FleetSpeed._
import not.ogame.bots._
import not.ogame.bots.selenium.EasySelenium._
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, JavascriptExecutor, WebDriver}

import scala.util.Random
import scala.jdk.CollectionConverters._

class SendFleetAction(webDriver: WebDriver) {
  def sendFleet(request: SendFleetRequest): Unit = {
    fillFleet1(request.ships)
    fillFleet2(request)
    fillFleet3(request.resources, request.fleetMissionType)
  }

  private def fillFleet1(ships: SendFleetRequestShips): Unit = {
    waitUntilVisible(By.id("fleet1"))
    webDriver.asInstanceOf[JavascriptExecutor].executeScript("javascript:window.scrollBy(0,1000)")
    Thread.sleep(200)
    ships match {
      case SendFleetRequestShips.AllShips => webDriver.findElement(By.id("sendall")).click()
      case SendFleetRequestShips.Ships(map) =>
        map
          .filter(_._2 > 0)
          .foreachEntry((shipType, number) => {
            webDriver.findElement(By.name(shipTypeToClassName(shipType))).sendKeys(number.toString)
            Thread.sleep(Random.nextLong(10) + 10)
          })
    }
    webDriver.findElement(By.id("continueToFleet2")).click()
  }

  private def fillFleet2(request: SendFleetRequest): Unit = {
    waitUntilVisible(By.id("fleet2"))
    selectSpeed(request.speed)
    fillTarget(request.targetCoordinates)
  }

  private def verifyDeuteriumAmount = {
    val overmarkElement = webDriver.findElement(By.id("consumption")).findElements(By.className("overmark")).asScala.headOption
    overmarkElement match {
      case Some(value) =>
        val requiredAmount = value.getText.split(" ").head.filter(_.isDigit).toInt
        throw AvailableDeuterExceeded(requiredAmount)
      case None => // do nothing
    }
  }

  private def selectSpeed(speedLevel: FleetSpeed): Unit = {
    webDriver.findElementsS(By.className("step")).find(_.getText == getSpeedLevelText(speedLevel)).get.click()
  }

  private def fillTarget(targetCoordinates: Coordinates): Unit = {
    val galaxy = webDriver.findElement(By.id("galaxy"))
    galaxy.clear()
    galaxy.sendKeys(targetCoordinates.galaxy.toString)
    val system = webDriver.findElement(By.id("system"))
    system.clear()
    system.sendKeys(targetCoordinates.system.toString)
    val position = webDriver.findElement(By.id("position"))
    position.clear()
    position.sendKeys(targetCoordinates.position.toString)
    webDriver.findElement(By.id(buttonCoordinatesType(targetCoordinates.coordinatesType))).click()
    verifyDeuteriumAmount
    waitUntilIsOn(By.id("continueToFleet3"))
    webDriver.findElement(By.id("continueToFleet3")).click()
  }

  def fillFleet3(resources: FleetResources, fleetMissionType: FleetMissionType): Unit = {
    waitUntilVisible(By.id("fleet3"))
    webDriver.findElement(By.id(buttonFleetMissionType(fleetMissionType))).click()
    webDriver.asInstanceOf[JavascriptExecutor].executeScript("javascript:window.scrollBy(0,1000)")
    Thread.sleep(200)
    resources match {
      case FleetResources.Given(resources) =>
        webDriver.findElement(By.id("crystal")).sendKeys(resources.crystal.toString)
        webDriver.findElement(By.id("deuterium")).sendKeys(resources.deuterium.toString)
        webDriver.findElement(By.id("metal")).sendKeys(resources.metal.toString)
      case FleetResources.Max =>
        webDriver.findElement(By.id("selectMaxMetal")).click()
        webDriver.findElement(By.id("selectMaxCrystal")).click()
        webDriver.findElement(By.id("selectMaxDeuterium")).click()
    }
    waitUntilIsOn(By.id("sendFleet"))
    webDriver.findElement(By.id("sendFleet")).click()
    waitUntilInvisible(By.id("fleet3"))
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

  private def buttonCoordinatesType(coordinatesType: CoordinatesType): String = {
    coordinatesType match {
      case CoordinatesType.Planet => "pbutton"
      case CoordinatesType.Moon   => "mbutton"
      case CoordinatesType.Debris => "dbutton"
    }
  }

  private def buttonFleetMissionType(fleetMissionType: FleetMissionType): String = {
    fleetMissionType match {
      case FleetMissionType.Deployment   => "missionButton4"
      case FleetMissionType.Expedition   => "missionButton15"
      case FleetMissionType.Colonization => "missionButton7"
      case FleetMissionType.Transport    => "missionButton3"
      case FleetMissionType.Attack       => "missionButton1"
      case FleetMissionType.Spy          => "missionButton6"
      case FleetMissionType.Recycle      => "missionButton8"
      case FleetMissionType.Destroy      => "missionButton9"
      case FleetMissionType.Unknown      => throw new IllegalArgumentException("Cannot send fleet with unknown mission")
    }
  }

  private def getSpeedLevelText(speedLevel: FleetSpeed): String = {
    speedLevel match {
      case Percent10  => "10"
      case Percent20  => "20"
      case Percent30  => "30"
      case Percent40  => "40"
      case Percent50  => "50"
      case Percent60  => "60"
      case Percent70  => "70"
      case Percent80  => "80"
      case Percent90  => "90"
      case Percent100 => "100"
    }
  }

  private def waitUntilVisible(by: By): Unit = {
    new WebDriverWait(webDriver, 2).until(ExpectedConditions.visibilityOfElementLocated(by))
  }

  private def waitUntilInvisible(by: By): Unit = {
    new WebDriverWait(webDriver, 2).until(ExpectedConditions.invisibilityOfElementLocated(by))
  }

  private def waitUntilIsOn(by: By): Unit = {
    new WebDriverWait(webDriver, 2).until(ExpectedConditions.attributeContains(by, "class", "on"))
  }
}
