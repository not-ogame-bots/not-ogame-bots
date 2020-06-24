package not.ogame.bots.selenium

import not.ogame.bots._
import not.ogame.bots.selenium.EasySelenium._
import org.openqa.selenium.{By, WebDriver}

class FleetDispatchComponentReader(webDriver: WebDriver)(implicit clock: LocalClock) {
  def readSlots(): FleetSlots = {
    webDriver.waitForElement(By.id("slots"))
    val advices = webDriver.findElement(By.id("slots")).findElementsS(By.className("advice"))
    val fleets = advices.head.getText.split("/").map(_.filter(_.isDigit).toInt)
    val expeditions = advices(1).getText.split("/").map(_.filter(_.isDigit).toInt)
    val tradeFleets = advices(2).getText.split("/").map(_.filter(_.isDigit).toInt)
    FleetSlots(
      currentFleets = fleets(0),
      maxFleets = fleets(1),
      currentExpeditions = expeditions(0),
      maxExpeditions = expeditions(1),
      currentTradeFleets = tradeFleets(0),
      maxTradeFleets = tradeFleets(1)
    )
  }
}
