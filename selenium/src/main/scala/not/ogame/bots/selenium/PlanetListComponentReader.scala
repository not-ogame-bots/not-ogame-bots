package not.ogame.bots.selenium

import cats.effect.{IO, Timer}
import not.ogame.bots.selenium.ParsingUtils.parseCoordinates
import not.ogame.bots.selenium.WebDriverSyntax._
import not.ogame.bots.selenium.WebDriverUtils._
import not.ogame.bots.{PlayerPlanet, _}
import org.openqa.selenium.{By, WebDriver, WebElement}

class PlanetListComponentReader(implicit val webDriver: WebDriver, implicit val timer: Timer[IO]) {
  def readPlanetList(): IO[List[PlayerPlanet]] =
    for {
      _ <- waitForElement(By.id("planetList"))
      list <- find(By.id("planetList"))
      planets <- list.findMany(By.xpath("*"))
      result = planets.map(element => PlayerPlanet(getPlanetId(element), getCoordinates(element)))
    } yield result

  private def getPlanetId(element: WebElement): String = element.getAttribute("id").stripPrefix("planet-")

  private def getCoordinates(element: WebElement): Coordinates = {
    val coordinatesText = element.findElement(By.className("planet-koords")).getText
    parseCoordinates(coordinatesText)
  }
}
