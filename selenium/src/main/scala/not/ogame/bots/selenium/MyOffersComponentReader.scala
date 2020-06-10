package not.ogame.bots.selenium

import not.ogame.bots.OfferItemType.{Crystal, Deuterium, Metal}
import not.ogame.bots._
import not.ogame.bots.selenium.EasySelenium._
import org.openqa.selenium.{By, WebDriver, WebElement}

class MyOffersComponentReader(webDriver: WebDriver)(implicit clock: LocalClock) {
  def readMyOffers(): List[MyOffer] = {
    webDriver.waitForElement(By.className("items"))
    val items = webDriver.findElement(By.className("items")).findElementsS(By.className("item"))
    items.map(item => readMyOffer(item))
  }

  private def readMyOffer(item: WebElement): MyOffer = {
    val details = item.findElement(By.className("details"))
    val price = item.findElement(By.className("price"))
    MyOffer(
      offerItemType = parseOfferItemType(details.findElement(By.tagName("h3")).getText),
      amount = details.findElement(By.className("quantity")).getText.filter(_.isDigit).toInt,
      priceItemType = parseOfferItemType(price.findElement(By.tagName("h3")).getText),
      price = price.findElement(By.className("quantity")).getText.filter(_.isDigit).toInt
    )
  }

  private def parseOfferItemType(text: String): OfferItemType = {
    text match {
      case "Metal"    => Metal
      case "Kryształ" => Crystal
      case "Deuter"   => Deuterium
    }
  }
}
