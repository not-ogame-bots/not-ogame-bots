package not.ogame.bots.selenium

import cats.effect.{Sync, Timer}
import cats.implicits._
import not.ogame.bots.selenium.WebDriverSyntax.testToInt
import org.openqa.selenium.{By, WebDriver, WebElement}

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object WebDriverUtils {
  implicit class RichWebElement[F[_]: Sync](webElement: WebElement) {
    def find(by: By): F[WebElement] = Sync[F].delay(webElement.findElement(by))

    def findMany(by: By): F[List[WebElement]] = Sync[F].delay(webElement.findElements(by).asScala.toList)

    def typeText(keys: String): F[Unit] = Sync[F].delay(webElement.sendKeys(keys))

    def clickF(): F[Unit] = Sync[F].delay(webElement.click())

    def sendKeysF(keys: String): F[Unit] = Sync[F].delay(webElement.sendKeys(keys))

    def readText: F[String] = Sync[F].delay(webElement.getText)

    def readInt(by: By): F[Int] = {
      find(by).map { component =>
        testToInt(component.getText)
      }
    }
  }

  implicit class RichWebDriver[F[_]: Sync: Timer](webDriver: WebDriver) {
    def closeF(): F[Unit] = Sync[F].delay(webDriver.close())

    def waitForElementF(by: By, attempts: Int = 100): F[WebElement] =
      waitForElementsF(by, attempts).map(_.head)

    def waitForElementsF(by: By, attempts: Int = 100): F[List[WebElement]] =
      findMany(by).flatMap {
        case l @ _ :: _          => Sync[F].pure(l)
        case Nil if attempts > 0 => Timer[F].sleep(100 millis) >> waitForElementsF(by, attempts - 1)
        case _                   => Sync[F].raiseError(TimeoutWaitingForElementBy(by))
      }

    def switchToOtherTab(): F[Unit] = Sync[F].delay {
      val otherHandle = webDriver.getWindowHandles.asScala.find(_ != webDriver.getWindowHandle)
      otherHandle.map(webDriver.switchTo().window(_))
    }

    def switchToAnyOpenTab(): F[Unit] = Sync[F].delay {
      webDriver.getWindowHandles.asScala.toList match {
        case ::(head, _) => webDriver.switchTo().window(head)
        case Nil         =>
      }
    }

    def safeUrlF(url: String, attempts: Int = 3): F[Unit] = {
      goto(url).flatMap { _ =>
        if (webDriver.getCurrentUrl != url) {
          if (attempts > 0) {
            Timer[F].sleep(10 millis) >> safeUrlF(url, attempts - 1)
          } else {
            Sync[F].raiseError(CouldNotProceedToUrl(url))
          }
        } else {
          Sync[F].unit
        }
      }
    }

    def find(by: By): F[WebElement] =
      Sync[F].delay(webDriver.findElement(by))

    def readInt(by: By): F[Int] = {
      find(by).map { component =>
        testToInt(component.getText)
      }
    }

    def findMany(by: By): F[List[WebElement]] =
      Sync[F].delay(webDriver.findElements(by).asScala.toList)

    def clickF(by: By): F[Unit] = find(by).flatMap(_.clickF())

    def goto(url: String): F[Unit] = Sync[F].delay(webDriver.get(url))
  }
}

object WebDriverSyntax {
  def testToInt(text: String): Int = {
    val number = text.filter(_.isDigit).toInt
    if (text.startsWith("-")) {
      -number
    } else {
      number
    }
  }
}
