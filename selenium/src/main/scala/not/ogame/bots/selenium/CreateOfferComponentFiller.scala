package not.ogame.bots.selenium

import not.ogame.bots.OfferItemType.{Deuterium, Metal}
import not.ogame.bots._
import not.ogame.bots.selenium.EasySelenium._
import org.openqa.selenium.{By, Keys, WebDriver}

class CreateOfferComponentFiller(webDriver: WebDriver)(implicit clock: LocalClock) {
  def createOffer(newOffer: MyOffer): Unit = {
    assert(newOffer.offerItemType == Metal, "Only selling metal is supported")
    webDriver.waitForElement(By.id("submitOffer"))
    webDriver.findElementsS(By.tagName("label")).find(_.getAttribute("for") == "btnOptResources").get.click()
    webDriver.findElement(By.id("quantity")).sendKeys(newOffer.amount.toString)
    webDriver.switchTo().activeElement().sendKeys(Keys.TAB)
    if (newOffer.priceItemType == Deuterium) {
      webDriver.switchTo().activeElement().sendKeys(Keys.ARROW_DOWN)
    }
    webDriver.switchTo().activeElement().sendKeys(Keys.TAB)
    webDriver.switchTo().activeElement().sendKeys(newOffer.price.toString)
    webDriver.findElement(By.id("submitOffer")).click()
  }
}
