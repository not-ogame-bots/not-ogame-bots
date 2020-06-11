package not.ogame.bots.selenium

import not.ogame.bots.OfferItemType.{Crystal, Deuterium, Metal}
import not.ogame.bots._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

class MyOffersComponentReaderSpec extends munit.FunSuite {
  implicit val clock: LocalClock = new RealLocalClock()

  private val driverFixture = FunFixture[WebDriver](
    setup = { _ =>
      new FirefoxDriver()
    },
    teardown = { driver =>
      driver.close()
    }
  )

  driverFixture.test("Should read my offers list") { driver =>
    driver.get(getClass.getResource("/my_offers_component_reader/my_offers.html").toURI.toString)
    val myOffers = testRead(driver)
    assertEquals(myOffers.size, 10)
    val first = myOffers.head
    assertEquals(first.offerItemType, Metal)
    assertEquals(first.amount, 1_000_000)
    assertEquals(first.priceItemType, Deuterium)
    assertEquals(first.price, 280_000)
    val sixth = myOffers(5)
    assertEquals(sixth.offerItemType, Metal)
    assertEquals(sixth.amount, 1_000_000)
    assertEquals(sixth.priceItemType, Crystal)
    assertEquals(sixth.price, 420_000)
  }

  driverFixture.test("Should read empty offers list") { driver =>
    driver.get(getClass.getResource("/my_offers_component_reader/no_my_offers.html").toURI.toString)
    val myOffers = testRead(driver)
    assertEquals(myOffers.size, 0)
  }

  private def testRead(driver: WebDriver): List[MyOffer] =
    new MyOffersComponentReader(driver).readMyOffers()
}
