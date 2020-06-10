package not.ogame.bots.selenium

import java.time.ZonedDateTime

import not.ogame.bots._
import not.ogame.bots.selenium.EasySelenium._
import org.openqa.selenium.{By, WebDriver, WebElement}

class MyFleetsComponentReader(webDriver: WebDriver)(implicit clock: LocalClock) {
  def readMyFleets(): List[MyFleet] = {
    val elements = webDriver.findElementsS(By.className("fleetDetails"))
    elements.map(readMyFleet)
  }

  private def readMyFleet(fleetElement: WebElement): MyFleet = {
    MyFleet(
      getFleetId(fleetElement),
      getArrivalTime(fleetElement),
      getFleetMissionType(fleetElement),
      getFrom(fleetElement),
      getTo(fleetElement),
      getFleetDataReturnFlight(fleetElement),
      getShips(fleetElement)
    )
  }

  private def getFleetId(fleetElement: WebElement): FleetId = {
    FleetId.apply(fleetElement.getAttribute("id"))
  }

  private def getArrivalTime(fleetElement: WebElement): ZonedDateTime = {
    val timeText = fleetElement.findElement(By.className("absTime")).getText.filter(c => c.isDigit || c == ':')
    ParsingUtils.parseTimeInFuture(timeText)
  }

  private def getFrom(fleetElement: WebElement): Coordinates = {
    val coordinatesText = fleetElement.findElement(By.className("originCoords")).getText
    val coordinatesTypeText =
      fleetElement.findElement(By.className("originPlanet")).findElement(By.className("planetIcon")).getAttribute("class")
    ParsingUtils.parseCoordinates(coordinatesText).copy(coordinatesType = parseCoordinatesType(coordinatesTypeText))
  }

  private def getTo(fleetElement: WebElement): Coordinates = {
    val coordinatesText = fleetElement.findElement(By.className("destinationCoords")).getText
    val destinationPlanet = fleetElement.findElement(By.className("destinationPlanet"))
    val coordinates = ParsingUtils.parseCoordinates(coordinatesText)
    if (destinationPlanet.findElementsS(By.className("planetIcon")).isEmpty) {
      //Expedition target 1:1:16 does not contain planetIcon but it is a Planet
      coordinates
    } else {
      val coordinatesTypeClassAttribute =
        destinationPlanet.findElement(By.className("planetIcon")).getAttribute("class")
      coordinates.copy(coordinatesType = parseCoordinatesType(coordinatesTypeClassAttribute))
    }
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

  private def getFleetMissionType(fleetElement: WebElement): FleetMissionType = {
    val missionTextLocalized = fleetElement.findElement(By.className("mission")).getText
    if (missionTextLocalized.contains("Stacjonuj")) {
      FleetMissionType.Deployment
    } else if (missionTextLocalized.contains("Ekspedycja")) {
      FleetMissionType.Expedition
    } else {
      FleetMissionType.Unknown
    }
  }

  private def getFleetDataReturnFlight(fleetElement: WebElement): Boolean = {
    val missionTextLocalized = fleetElement.findElement(By.className("mission")).getText
    missionTextLocalized.contains("(R)")
  }

  private def getShips(fleetElement: WebElement): Map[ShipType, Int] = {
    val fleetInfoTooltip = fleetElement.findElement(By.className("fleetinfo"))
    val rows = fleetInfoTooltip.findElementsS(By.tagName("tr"))
    val value: List[Option[(ShipType, Int)]] = rows.map(row => {
      val rowText = row.getAttribute("textContent")
      getShipType(rowText).map(shipType => {
        val count = row.findElement(By.className("value")).getAttribute("textContent").filter(_.isDigit).toInt
        (shipType, count)
      })
    })
    value.flatten.toMap.withDefaultValue(0)
  }

  private def getShipType(rowText: String): Option[ShipType] = {
    nameOfShips().collectFirst { case (shipType, shipName) if rowText.contains(shipName) => shipType }
  }

  private def nameOfShips(): Map[ShipType, String] = {
    ShipType.values.map(shipType => shipType -> localizedNameOfAShipType(shipType)).toMap
  }

  private def localizedNameOfAShipType(shipType: ShipType): String = {
    shipType match {
      case ShipType.LightFighter   => "Lekki myśliwiec"
      case ShipType.HeavyFighter   => "Ciężki myśliwiec"
      case ShipType.Cruiser        => "Krążownik"
      case ShipType.Battleship     => "Okręt wojenny"
      case ShipType.Interceptor    => "Pancernik"
      case ShipType.Bomber         => "Bombowiec"
      case ShipType.Destroyer      => "Niszczyciel"
      case ShipType.DeathStar      => "Gwiazda Śmierci"
      case ShipType.Reaper         => "Rozpruwacz"
      case ShipType.Explorer       => "Pionier"
      case ShipType.SmallCargoShip => "Mały transporter"
      case ShipType.LargeCargoShip => "Duży transporter"
      case ShipType.ColonyShip     => "Statek kolonizacyjny"
      case ShipType.Recycler       => "Recykler"
      case ShipType.EspionageProbe => "Sonda szpiegowska"
    }
  }
}
