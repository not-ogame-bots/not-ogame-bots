package not.ogame.bots.ordon.action

import java.time.ZonedDateTime

import not.ogame.bots.ordon.core.{EventRegistry, OrdonOgameDriver, TimeBasedOrdonAction}
import not.ogame.bots.ordon.utils.SlackIntegration

class ChatRelayOrdonAction extends TimeBasedOrdonAction {
  private val slackIntegration = new SlackIntegration()

  override def processTimeBased(ogame: OrdonOgameDriver, eventRegistry: EventRegistry): ZonedDateTime = {
    val allianceMessages = ogame.readAllianceMessages()
    val chatConversations = ogame.readChatConversations()

    val newMessages = allianceMessages.filter(_.date.isAfter(ZonedDateTime.now().minusMinutes(4)))
    val newChatMessages = chatConversations.filter(_.lastDate.isAfter(ZonedDateTime.now().minusMinutes(4)))

    val messages = newMessages.map(m => s"${m.from}: ${m.message}") ++ newChatMessages.map(m => s"${m.conversationWith}: ${m.lastMessage}")
    slackIntegration.postRelayMessageToSlack(messages.mkString("\n"))
    ZonedDateTime.now().plusMinutes(3)
  }
}
