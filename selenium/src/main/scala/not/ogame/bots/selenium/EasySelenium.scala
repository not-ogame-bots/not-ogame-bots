package not.ogame.bots.selenium

import org.openqa.selenium.{By, WebDriver, WebElement}

import scala.jdk.CollectionConverters._

object EasySelenium {
  implicit class EasyWebDriver(webDriver: WebDriver) {
    def safeUrl(url: String, attempts: Int = 3): Unit = {
      webDriver.get(url)
      if (webDriver.getCurrentUrl != url) {
        if (attempts > 0) {
          safeUrl(url, attempts - 1)
        } else {
          throw new RuntimeException(s"Couldn't proceed to page $url")
        }
      }
    }

    @scala.annotation.tailrec
    final def waitForElement(by: By, attempts: Int = 100): Unit = {
      val elements = webDriver.findElements(by)
      if (elements.isEmpty) {
        if (attempts > 0) {
          Thread.sleep(100)
          waitForElement(by, attempts - 1)
        } else {
          throw new RuntimeException(s"Time out waiting for $by")
        }
      }
    }

    def findElementsS(by: By): List[WebElement] = {
      webDriver.findElements(by).asScala.toList
    }
  }

  implicit class EasyWebElement(webElement: WebElement) {
    @scala.annotation.tailrec
    final def waitForElement(by: By, attempts: Int = 100): Unit = {
      val elements = webElement.findElements(by)
      if (elements.isEmpty) {
        if (attempts > 0) {
          Thread.sleep(100)
          waitForElement(by, attempts - 1)
        } else {
          throw new RuntimeException(s"Time out waiting for $by")
        }
      }
    }

    def findElementsS(by: By): List[WebElement] = {
      webElement.findElements(by).asScala.toList
    }
  }
}
