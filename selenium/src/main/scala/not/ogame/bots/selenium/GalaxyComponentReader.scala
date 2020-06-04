package not.ogame.bots.selenium

import enumeratum.{Enum, EnumEntry}
import not.ogame.bots.CoordinatesType.Moon
import not.ogame.bots.selenium.EasySelenium._
import not.ogame.bots.selenium.PlayerActivity.{LessThan15MinutesAgo, MinutesAgo, NotActive}
import not.ogame.bots.{Coordinates, LocalClock}
import org.openqa.selenium.{By, WebDriver, WebElement}

class GalaxyComponentReader(webDriver: WebDriver)(implicit clock: LocalClock) {
  def readGalaxyPage(): Map[Coordinates, PlayerActivity] = {
    webDriver.waitForElement(By.id("galaxytable"))
    val galaxyTable: WebElement = webDriver.findElement(By.id("galaxytable"))
    val galaxy = galaxyTable.getAttribute("data-galaxy").toInt
    val system = galaxyTable.getAttribute("data-system").toInt
    val rows = galaxyTable.findElement(By.tagName("tbody")).findElementsS(By.className("row"))
    rows.flatMap(row => parseRow(galaxy, system, row)).toMap
  }

  private def parseRow(galaxy: Int, system: Int, row: WebElement): List[(Coordinates, PlayerActivity)] = {
    val position = row.findElement(By.className("position")).getText.toInt
    val isColonized = row.findElementsS(By.className("colonized")).nonEmpty
    val hasMoon = row.findElementsS(By.className("moon")).nonEmpty
    if (!isColonized) {
      List()
    } else if (!hasMoon) {
      List((Coordinates(galaxy, system, position), parsePlanetActivity(row)))
    } else {
      List(
        (Coordinates(galaxy, system, position), parsePlanetActivity(row)),
        (Coordinates(galaxy, system, position, Moon), parseMoonActivity(row))
      )
    }
  }

  private def parsePlanetActivity(row: WebElement): PlayerActivity = {
    val planetElement = row.findElement(By.className("colonized"))
    if (planetElement.findElementsS(By.className("minute15")).nonEmpty) {
      LessThan15MinutesAgo
    } else if (planetElement.findElementsS(By.className("showMinutes")).nonEmpty) {
      MinutesAgo(planetElement.findElement(By.className("showMinutes")).getText.toInt)
    } else {
      NotActive
    }
  }

  private def parseMoonActivity(row: WebElement): PlayerActivity = {
    val moonElement = row.findElement(By.className("moon"))
    if (moonElement.findElementsS(By.className("minute15")).nonEmpty) {
      LessThan15MinutesAgo
    } else if (moonElement.findElementsS(By.className("showMinutes")).nonEmpty) {
      MinutesAgo(moonElement.findElement(By.className("showMinutes")).getText.toInt)
    } else {
      NotActive
    }
  }
}

sealed trait PlayerActivity extends EnumEntry

object PlayerActivity extends Enum[PlayerActivity] {
  case object LessThan15MinutesAgo extends PlayerActivity

  case class MinutesAgo(minutes: Int) extends PlayerActivity

  case object NotActive extends PlayerActivity

  override def values: IndexedSeq[PlayerActivity] = findValues
}
