package not.ogame.bots.selenium

import java.time.ZonedDateTime

import not.ogame.bots._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

class ChatMessagesReaderSpec extends munit.FunSuite with GecoDriver {
  implicit val clock: LocalClock = new RealLocalClock()

  private val driverFixture = FunFixture[WebDriver](
    setup = { _ =>
      new FirefoxDriver()
    },
    teardown = { driver =>
      driver.close()
    }
  )

  driverFixture.test("Should read alliance messages") { driver =>
    driver.get(getClass.getResource("/chat_messages_reader/messages_alliance.html").toURI.toString)
    val conversations = testReadAllianceMessages(driver)
    assertEquals(conversations.size, 10)
    val firstMessage = conversations.head
    assertEquals(firstMessage.id, "2426462")
    assertEquals(firstMessage.from, "Ordon")
    assertEquals(firstMessage.date, ZonedDateTime.parse("2020-07-11T17:57:25+02:00"))
    assertEquals(firstMessage.message, "Napisałem. To po tym odszedł.")
  }

  private def testReadAllianceMessages(driver: WebDriver): List[ChatMessage] =
    new ChatMessageReader(driver).readAllianceMessages()

  driverFixture.test("Should read chat messages") { driver =>
    driver.get(getClass.getResource("/chat_messages_reader/messages_chat.html").toURI.toString)
    val conversations = testReadChatConversations(driver)
    assertEquals(conversations.size, 11)
    val firstConversation = conversations.head
    assertEquals(firstConversation.id, "500023")
    assertEquals(firstConversation.conversationWith, "LEN Czat sojuszu")
    assertEquals(firstConversation.lastDate, ZonedDateTime.parse("2020-07-11T17:09:45+02:00"))
    assertEquals(firstConversation.lastMessage.contains("Ordon"), true)
    val secondConversation = conversations(1)
    assertEquals(secondConversation.id, "104179")
    assertEquals(secondConversation.conversationWith, "DarthPiter [2:391:10]")
    assertEquals(secondConversation.lastDate, ZonedDateTime.parse("2020-07-11T17:53:40+02:00"))
    assertEquals(secondConversation.lastMessage, "dzięki za checi")
  }

  private def testReadChatConversations(driver: WebDriver): List[ChatConversations] =
    new ChatMessageReader(driver).readChatConversations()
}
