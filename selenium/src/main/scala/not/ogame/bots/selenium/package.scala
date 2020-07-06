package not.ogame.bots

import org.openqa.selenium.By

package object selenium {
  case class TimeoutWaitingForElementBy(by: By) extends TimeoutWaitingForElement(s"Time out waiting for $by")

  case class TimeoutWaitingForElementByPredicate() extends TimeoutWaitingForElement(s"Time out waiting for element by predicate")

  class TimeoutWaitingForElement(message: String) extends RuntimeException(message)

  case class CouldNotProceedToUrl(url: String) extends RuntimeException(s"Couldn't proceed to page $url")
}
