package not.ogame.bots.selenium

import cats.effect.IO
import org.openqa.selenium.{By, WebDriver, WebElement}

import scala.jdk.CollectionConverters._

object WebDriverUtils {

  implicit class RichWebElement(webElement: WebElement) {

    def find(by: By): IO[Option[WebElement]] =
      IO.delay(Option(webElement.findElement(by)))

    def findMany(by: By): IO[List[WebElement]] =
      IO.delay(webElement.findElements(by).asScala.toList)

    def typeText(keys: String): IO[Unit] = {
      IO.delay(webElement.sendKeys(keys))
    }

    def clickF(): IO[Unit] = {
      IO.delay(webElement.click())
    }
  }
}

object WebDriverSyntax {

  //TODO
  def waitForElement(by: By)(implicit webDriver: WebDriver): IO[WebElement] =
    IO.delay {
      val startTime = System.currentTimeMillis()
      while (webDriver.findElements(by).isEmpty) {
        if (System.currentTimeMillis() - startTime > 10_000) {
          throw new RuntimeException("Timeout waiting for element to become available: $by")
        }
        Thread.sleep(10)
      }
      webDriver.findElement(by)
    }

  //TODO
  def waitForElements(by: By)(implicit webDriver: WebDriver): IO[List[WebElement]] = IO.delay {
    val startTime = System.currentTimeMillis()
    while (webDriver.findElements(by).isEmpty) {
      if (System.currentTimeMillis() - startTime > 10_000) {
        throw new RuntimeException("Timeout waiting for element to become available: $by")
      }
      Thread.sleep(10)
    }
    webDriver.findElements(by).asScala.toList
  }

  def switchToOtherTab()(implicit webDriver: WebDriver): IO[Unit] = IO.delay {
    val otherHandle = webDriver.getWindowHandles.asScala.find(_ != webDriver.getWindowHandle).get
    webDriver.switchTo().window(otherHandle)
  }

  def switchToAnyOpenTab()(implicit webDriver: WebDriver): IO[Unit] = IO.delay {
    val firstHandle = webDriver.getWindowHandles.asScala.head
    webDriver.switchTo().window(firstHandle)
  }

  def safeUrl(url: String)(implicit webDriver: WebDriver): IO[Unit] = {
    (go to url).flatMap { _ =>
      if (webDriver.getCurrentUrl != url) {
        safeUrl(url)
      } else {
        IO.unit
      }
    }
  }

  def find(by: By)(implicit webDriver: WebDriver): IO[Option[WebElement]] =
    IO.delay(Option(webDriver.findElement(by)))

  def findMany(by: By)(implicit webDriver: WebDriver): IO[List[WebElement]] =
    IO.delay(webDriver.findElements(by).asScala.toList)

  object go {

    def to(url: String)(implicit driver: WebDriver): IO[Unit] = {
      IO.delay(driver.get(url))
    }
  }
}
