package not.ogame.bots.selenium

import not.ogame.bots.CoordinatesType.{Debris, Moon}
import not.ogame.bots.PlayerActivity.{LessThan15MinutesAgo, MinutesAgo, NotActive}
import not.ogame.bots._
import not.ogame.bots.selenium.EasySelenium._
import org.openqa.selenium.{By, WebDriver, WebElement}

class GalaxyComponentReader(webDriver: WebDriver)(implicit clock: LocalClock) {
  def readGalaxyPage(): GalaxyPageData = {
    webDriver.waitForElement(By.id("galaxytable"))

    val galaxyTable: WebElement = webDriver.findElement(By.id("galaxytable"))
    val galaxy = galaxyTable.getAttribute("data-galaxy").toInt
    val system = galaxyTable.getAttribute("data-system").toInt
    val rowsData = galaxyTable
      .findElement(By.tagName("tbody"))
      .findElementsS(By.className("row"))
      .map(row => parseRow(row)) ++ List(readExpeditionDebrisSlotBox(galaxyTable))
    val planetsActivities: Seq[(Coordinates, PlayerActivity)] =
      rowsData.flatMap(
        rowData => rowData.planetActivity.map(planetActivity => Coordinates(galaxy, system, rowData.position) -> planetActivity)
      )
    val moonActivities: Seq[(Coordinates, PlayerActivity)] =
      rowsData.flatMap(
        rowData => rowData.moonActivity.map(moonActivity => Coordinates(galaxy, system, rowData.position, Moon) -> moonActivity)
      )
    val debris: Seq[(Coordinates, Resources)] =
      rowsData.flatMap(rowData => rowData.debris.map(debris => Coordinates(galaxy, system, rowData.position, Debris) -> debris))
    GalaxyPageData((planetsActivities ++ moonActivities).toMap, debris.toMap)
  }

  def parseRow(row: WebElement): RowData = {
    RowData(
      position = row.findElement(By.className("position")).getText.toInt,
      planetActivity = parsePlanetActivity(row),
      moonActivity = parseMoonActivity(row),
      debris = parseDebrisActivity(row)
    )
  }

  private def parsePlanetActivity(row: WebElement): Option[PlayerActivity] = {
    row
      .findElementsS(By.className("colonized"))
      .headOption
      .map(
        planetElement =>
          if (planetElement.findElementsS(By.className("minute15")).nonEmpty) {
            LessThan15MinutesAgo
          } else if (planetElement.findElementsS(By.className("showMinutes")).nonEmpty) {
            MinutesAgo(planetElement.findElement(By.className("showMinutes")).getText.toInt)
          } else {
            NotActive
          }
      )
  }

  private def parseMoonActivity(row: WebElement): Option[PlayerActivity] = {
    row
      .findElementsS(By.className("moon"))
      .find(moonElement => moonElement.getAttribute("data-moon-id") != null)
      .map(
        moonElement =>
          if (moonElement.findElementsS(By.className("minute15")).nonEmpty) {
            LessThan15MinutesAgo
          } else if (moonElement.findElementsS(By.className("showMinutes")).nonEmpty) {
            MinutesAgo(moonElement.findElement(By.className("showMinutes")).getText.toInt)
          } else {
            NotActive
          }
      )
  }

  private def parseDebrisActivity(row: WebElement): Option[Resources] = {
    row
      .findElementsS(By.className("debris"))
      .find(debrisElement => !debrisElement.getAttribute("class").contains("js_no_action"))
      .map(_ => Resources(0, 0, 0))
  }

  private def readExpeditionDebrisSlotBox(galaxyTable: WebElement): RowData = {
    RowData(
      16,
      Option.empty,
      Option.empty,
      galaxyTable.findElementsS(By.className("expeditionDebrisSlotBox")).headOption.map(_ => Resources(0, 0, 0))
    )
  }

  case class RowData(position: Int, planetActivity: Option[PlayerActivity], moonActivity: Option[PlayerActivity], debris: Option[Resources])
}
