package not.ogame.bots.selenium

import not.ogame.bots.selenium.EasySelenium._
import not.ogame.bots.selenium.ParsingUtils.parseCoordinates
import not.ogame.bots.{PlayerPlanet, _}
import org.openqa.selenium.{By, WebDriver, WebElement}
import scala.jdk.CollectionConverters._
class PlanetListComponentReader(webDriver: WebDriver) {
  private implicit class RegexOps(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  def readPlanetList(): List[PlayerPlanet] = {
    webDriver.waitForElement(By.id("planetList"))
    webDriver.findElement(By.id("planetList")).findElementsS(By.xpath("*")).flatMap(readPlanet)
  }

  def readMoon(element: WebElement, planet: PlayerPlanet): Option[PlayerPlanet] = {
    element
      .findElements(By.className("moonlink"))
      .asScala
      .map { moon =>
        val dataLink = moon.getAttribute("data-link")
        dataLink match {
          case r".*cp=(\w+)$w" =>
            planet.copy(id = PlanetId(w), coordinates = planet.coordinates.copy(coordinatesType = CoordinatesType.Moon))
          case _ => ???
        }
      }
      .headOption
  }

  private def readPlanet(element: WebElement): List[PlayerPlanet] = {
    val planet = PlayerPlanet(getPlanetId(element), getCoordinates(element))
    List(Some(planet), readMoon(element, planet)).flatten
  }

  private def getPlanetId(element: WebElement) = PlanetId(element.getAttribute("id").stripPrefix("planet-"))

  private def getCoordinates(element: WebElement): Coordinates = {
    val coordinatesText = element.findElement(By.className("planet-koords")).getText
    parseCoordinates(coordinatesText)
  }
}
