package not.ogame.bots.selenium

import not.ogame.bots.selenium.EasySelenium._
import not.ogame.bots.selenium.ParsingUtils.parseCoordinates
import not.ogame.bots.{PlayerPlanet, _}
import org.openqa.selenium.{By, WebDriver, WebElement}

class PlanetListComponentReader(webDriver: WebDriver) {
  def readPlanetList(): List[PlayerPlanet] = {
    webDriver.waitForElement(By.id("planetList"))
    webDriver.findElement(By.id("planetList")).findElementsS(By.xpath("*")).map(readPlanet)
  }

  private def readPlanet(element: WebElement): PlayerPlanet = PlayerPlanet(getPlanetId(element), getCoordinates(element))

  private def getPlanetId(element: WebElement) = PlanetId(element.getAttribute("id").stripPrefix("planet-"))

  private def getCoordinates(element: WebElement): Coordinates = {
    val coordinatesText = element.findElement(By.className("planet-koords")).getText
    parseCoordinates(coordinatesText)
  }
}
