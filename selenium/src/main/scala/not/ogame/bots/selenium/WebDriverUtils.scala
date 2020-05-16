package not.ogame.bots.selenium

import cats.effect.{IO, Timer}
import org.openqa.selenium.{By, WebDriver, WebElement}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import cats.implicits._

object WebDriverUtils {

  implicit class RichWebElement(webElement: WebElement) {
    def find(by: By): IO[WebElement] = IO.delay(webElement.findElement(by))

    def findMany(by: By): IO[List[WebElement]] = IO.delay(webElement.findElements(by).asScala.toList)

    def typeText(keys: String): IO[Unit] = IO.delay(webElement.sendKeys(keys))

    def clickF(): IO[Unit] = IO.delay(webElement.click())
  }

  implicit class RichWebDriver(webDriver: WebDriver) {
    def closeF(): IO[Unit] = IO.delay(webDriver.close())
  }
}

object WebDriverSyntax {

  def waitForElement(by: By, attempts: Int = 100)(implicit webDriver: WebDriver, timer: Timer[IO]): IO[WebElement] =
    waitForElements(by, attempts).map(_.head)

  def waitForElements(by: By, attempts: Int = 100)(implicit webDriver: WebDriver, timer: Timer[IO]): IO[List[WebElement]] =
    findMany(by).flatMap {
      case l @ _ :: _          => IO.pure(l)
      case Nil if attempts > 0 => IO.sleep(100 millis) >> waitForElements(by, attempts - 1)
      case _                   => IO.raiseError(new RuntimeException(s"Timeout waiting for element to become available: $by"))
    }

  def switchToOtherTab()(implicit webDriver: WebDriver): IO[Unit] = IO.delay {
    val otherHandle = webDriver.getWindowHandles.asScala.find(_ != webDriver.getWindowHandle)
    otherHandle.map(webDriver.switchTo().window(_))
  }

  def switchToAnyOpenTab()(implicit webDriver: WebDriver): IO[Unit] = IO.delay {
    webDriver.getWindowHandles.asScala.toList match {
      case ::(head, _) => webDriver.switchTo().window(head)
      case Nil         =>
    }
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

  def find(by: By)(implicit webDriver: WebDriver): IO[WebElement] =
    IO.delay(webDriver.findElement(by))

  def findMany(by: By)(implicit webDriver: WebDriver): IO[List[WebElement]] =
    IO.delay(webDriver.findElements(by).asScala.toList)

  object go {
    def to(url: String)(implicit driver: WebDriver): IO[Unit] = IO.delay(driver.get(url))
  }
}
