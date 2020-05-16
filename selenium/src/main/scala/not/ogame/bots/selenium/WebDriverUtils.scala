package not.ogame.bots.selenium

import java.util

import org.openqa.selenium.{By, WebDriver, WebElement}

import scala.jdk.CollectionConverters._

object WebDriverUtils {

  implicit class WebDriverImprovements(val webDriver: WebDriver) {

    def waitForElement(by: By): WebElement = {
      val startTime = System.currentTimeMillis()
      while (webDriver.findElements(by).isEmpty) {
        if (System.currentTimeMillis() - startTime > 10_000) {
          throw new RuntimeException("Timeout waiting for element to become available: $by")
        }
        Thread.sleep(10)
      }
      webDriver.findElement(by)
    }

    def waitForElements(by: By): util.List[WebElement] = {
      val startTime = System.currentTimeMillis()
      while (webDriver.findElements(by).isEmpty) {
        if (System.currentTimeMillis() - startTime > 10_000) {
          throw new RuntimeException("Timeout waiting for element to become available: $by")
        }
        Thread.sleep(10)
      }
      webDriver.findElements(by)
    }

    def switchToOtherTab(): Unit = {
      val otherHandle = webDriver.getWindowHandles.asScala.find(_ != webDriver.getWindowHandle).get
      webDriver.switchTo().window(otherHandle)
    }

    def switchToAnyOpenTab(): Unit = {
      val firstHandle = webDriver.getWindowHandles.asScala.head
      webDriver.switchTo().window(firstHandle)
    }

    def safeUrl(url: String): Unit = {
      webDriver.get(url)
      if (webDriver.getCurrentUrl != url) {
        webDriver.get(url)
      }
      if (webDriver.getCurrentUrl != url) {
        webDriver.get(url)
      }
    }
  }
}
