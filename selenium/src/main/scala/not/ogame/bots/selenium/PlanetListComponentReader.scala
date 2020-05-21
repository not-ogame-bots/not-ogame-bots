package not.ogame.bots.selenium

import cats.effect.{IO, Timer}
import not.ogame.bots.selenium.WebDriverSyntax._
import not.ogame.bots.selenium.WebDriverUtils._
import not.ogame.bots.{PlayerPlanet, _}
import org.openqa.selenium.{By, WebDriver, WebElement}

class PlanetListComponentReader(implicit val webDriver: WebDriver, implicit val timer: Timer[IO]) {
  def readPlanetList(): WaitAndProcess[List[PlayerPlanet]] =
    WaitAndProcess(readPlanetListAwait, readPlanetListProcess)

  private def readPlanetListAwait: IO[Unit] = {
    for {
      _ <- waitForElement(By.id("planetList"))
    } yield ()
  }

  private def readPlanetListProcess: IO[List[PlayerPlanet]] = {
    for {
      list <- find(By.id("planetList"))
      planets <- list.findMany(By.xpath("*"))
      result = planets.map(element => PlayerPlanet(getPlanetId(element), getCoordinates(element)))
    } yield result
  }

  private def getPlanetId(element: WebElement): String = element.getAttribute("id").stripPrefix("planet-")

  private def getCoordinates(element: WebElement): Coordinates = {
    val coordinates = element.findElement(By.className("planet-koords")).getText.split(":").map(_.filter(_.isDigit).toInt)
    Coordinates(coordinates(0), coordinates(1), coordinates(2))
  }
}

case class WaitAndProcess[T](await: IO[Unit], process: IO[T])
