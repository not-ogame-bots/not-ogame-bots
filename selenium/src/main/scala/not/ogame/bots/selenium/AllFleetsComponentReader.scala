package not.ogame.bots.selenium

import java.time.ZonedDateTime

import not.ogame.bots._
import not.ogame.bots.selenium.EasySelenium._
import org.openqa.selenium.{By, WebDriver, WebElement}

class AllFleetsComponentReader(webDriver: WebDriver)(implicit clock: LocalClock) {
  def readAllFleets(): List[Fleet] = {
    val elements = webDriver.findElementsS(By.className("eventFleet"))
    elements.map(readFleet)
  }

  private def readFleet(fleetElement: WebElement): Fleet = {
    Fleet(
      SimplifiedDataTime.from(getArrivalTime(fleetElement)),
      getFleetAttitude(fleetElement),
      getFleetMissionType(fleetElement),
      getFrom(fleetElement),
      getTo(fleetElement),
      getFleetDataReturnFlight(fleetElement)
    )
  }

  private def getFrom(fleetElement: WebElement): Coordinates = {
    val coordinatesText = fleetElement.findElement(By.className("coordsOrigin")).getText
    val coordinatesTypeText =
      fleetElement.findElement(By.className("originFleet")).findElement(By.className("planetIcon")).getAttribute("class")
    ParsingUtils.parseCoordinates(coordinatesText).copy(coordinatesType = parseCoordinatesType(coordinatesTypeText))
  }

  private def getTo(fleetElement: WebElement): Coordinates = {
    val coordinatesText = fleetElement.findElement(By.className("destCoords")).getText
    val coordinatesTypeClassAttribute =
      fleetElement.findElement(By.className("destFleet")).findElement(By.className("planetIcon")).getAttribute("class")
    ParsingUtils.parseCoordinates(coordinatesText).copy(coordinatesType = parseCoordinatesType(coordinatesTypeClassAttribute))
  }

  private def parseCoordinatesType(coordinatesTypeClassAttribute: String): CoordinatesType = {
    val classes = coordinatesTypeClassAttribute.split(" ")
    if (classes.contains("planet")) {
      CoordinatesType.Planet
    } else if (classes.contains("moon")) {
      CoordinatesType.Moon
    } else {
      CoordinatesType.Debris
    }
  }

  private def getArrivalTime(fleetElement: WebElement): ZonedDateTime = {
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
      case 7  => FleetMissionType.Colonization
      case 3  => FleetMissionType.Transport
      case _  => FleetMissionType.Unknown
    }
  }

  private def getFleetDataReturnFlight(fleetElement: WebElement): Boolean = {
    fleetElement.getAttribute("data-return-flight") == "true"
  }
}
