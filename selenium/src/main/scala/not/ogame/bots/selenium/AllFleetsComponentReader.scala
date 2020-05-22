package not.ogame.bots.selenium

import java.time.LocalDateTime

import not.ogame.bots._
import not.ogame.bots.selenium.EasySelenium._
import org.openqa.selenium.{By, WebDriver, WebElement}

class AllFleetsComponentReader(webDriver: WebDriver) {
  def readAllFleets(): List[Fleet] = {
    val elements = webDriver.findElementsS(By.className("eventFleet"))
    elements.map(readFleet)
  }

  private def readFleet(fleetElement: WebElement): Fleet = {
    Fleet(
      getArrivalTime(fleetElement),
      getFleetAttitude(fleetElement),
      getFleetMissionType(fleetElement),
      getFrom(fleetElement),
      getTo(fleetElement)
    )
  }

  private def getFrom(fleetElement: WebElement): Coordinates = {
    val coordinatesText = fleetElement.findElement(By.className("coordsOrigin")).getText
    ParsingUtils.parseCoordinates(coordinatesText)
  }

  private def getTo(fleetElement: WebElement): Coordinates = {
    val coordinatesText = fleetElement.findElement(By.className("destCoords")).getText
    ParsingUtils.parseCoordinates(coordinatesText)
  }

  private def getArrivalTime(fleetElement: WebElement): LocalDateTime = {
    val timeText = fleetElement.findElement(By.className("arrivalTime")).getText
    ParsingUtils.parseTimeInFuture(timeText)
  }

  private def getFleetAttitude(fleetElement: WebElement): FleetAttitude = {
    val elements = fleetElement.findElements(By.className("hostile"))
    if (!elements.isEmpty) FleetAttitude.Hostile else FleetAttitude.Friendly
  }

  private def getFleetMissionType(fleetElement: WebElement): FleetMissionType = {
    fleetElement.getAttribute("data-mission-type").toInt match {
      case 4  => FleetMissionType.Deployment
      case 15 => FleetMissionType.Expedition
      case _  => FleetMissionType.Unknown
    }
  }
}
