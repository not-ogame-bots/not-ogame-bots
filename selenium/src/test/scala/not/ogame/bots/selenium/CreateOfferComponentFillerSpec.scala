package not.ogame.bots.selenium

import not.ogame.bots.OfferItemType.{Deuterium, Metal}
import not.ogame.bots._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

class CreateOfferComponentFillerSpec extends munit.FunSuite {
  implicit val clock: LocalClock = new RealLocalClock()

  private val driverFixture = FunFixture[WebDriver](
    setup = { _ =>
      new FirefoxDriver()
    },
    teardown = { driver =>
      driver.close()
    }
  )

  driverFixture.test("Should fill new offer") { driver =>
    driver.get(getClass.getResource("/create_offer_component_filler/create_offer.html").toURI.toString)
    val newOffer = MyOffer(
      offerItemType = Metal,
      amount = 1_000_000,
      priceItemType = Deuterium,
      price = 280_000
    )
    testFilling(driver, newOffer)
  }

  private def testFilling(driver: WebDriver, newOffer: MyOffer) =
    new CreateOfferComponentFiller(driver).createOffer(newOffer)
}
