package not.ogame.bots.selenium

import not.ogame.bots.FleetSpeed._
import not.ogame.bots._
import not.ogame.bots.selenium.EasySelenium._
import org.openqa.selenium.{By, JavascriptExecutor, WebDriver}

import scala.util.Random

class SendFleetAction(webDriver: WebDriver) {
  def sendFleet(request: SendFleetRequest): Unit = {
    fillFleet(request.ships)
    selectSpeed(request.speed)
    fillTarget(request.targetCoordinates)
    fillResources(request.resources, request.fleetMissionType)
  }

  private def fillFleet(ships: SendFleetRequestShips): Unit = {
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

  private def fillTarget(targetCoordinates: Coordinates): Unit = {
    webDriver.waitForElement(By.id("continueToFleet3"))
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
    Thread.sleep(Random.nextLong(10) + 10)
    webDriver.findElement(By.id("continueToFleet3")).click()
  }

  def fillResources(resources: FleetResources, fleetMissionType: FleetMissionType): Unit = {
    webDriver.waitForElement(By.id("sendFleet"))
    Thread.sleep(3_000)
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
    webDriver.findElement(By.id("sendFleet")).click()
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

  private def selectSpeed(speedLevel: FleetSpeed): Unit = {
    webDriver.waitForElement(By.className("step"))
    webDriver.findElementsS(By.className("step")).find(_.getText == getSpeedLevelText(speedLevel)).get.click()
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
}
