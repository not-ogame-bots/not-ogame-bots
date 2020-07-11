package not.ogame.bots.selenium

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZonedDateTime}

import not.ogame.bots.selenium.EasySelenium._
import not.ogame.bots.{ChatConversations, ChatMessage, LocalClock}
import org.openqa.selenium.{By, WebDriver}

class ChatMessageReader(webDriver: WebDriver)(implicit clock: LocalClock) {
  def readAllianceMessages(): List[ChatMessage] = {
    webDriver
      .findElementsS(By.className("msg"))
      .map(msg => {
        ChatMessage(
          id = msg.getAttribute("data-msg-id"),
          from = msg.findElement(By.className("msg_sender")).getAttribute("textContent"),
          date = parseDate(msg.findElement(By.className("msg_date")).getAttribute("textContent")),
          message = msg.findElement(By.className("msg_content")).getAttribute("textContent").trim
        )
      })
  }

  def readChatConversations(): List[ChatConversations] = {
    webDriver.waitForElement(By.id("chatMsgList"))
    webDriver
      .findElementsS(By.className("msg"))
      .map(msg => {
        val associationId = msg.getAttribute("data-associationid")
        val playerId = msg.getAttribute("data-playerid")
        ChatConversations(
          id = if (associationId != null) associationId else playerId,
          conversationWith = msg.findElement(By.className("msg_title")).getAttribute("textContent").trim.replaceAll("[ \n]+", " "),
          lastDate = parseDate(msg.findElement(By.className("msg_date")).getAttribute("textContent")),
          lastMessage = msg.findElement(By.className("msg_content")).getAttribute("textContent").trim
        )
      })
  }

  private def parseDate(dateString: String): ZonedDateTime = {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    val dateTime = LocalDateTime.parse(dateString, formatter)
    ZonedDateTime.of(dateTime, clock.now().getZone)
  }
}
