package not.ogame.bots.ordon.utils

import com.google.gson.GsonBuilder
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.{Body, POST, Url}
import retrofit2.{Call, Retrofit}

import scala.io.Source

class SlackIntegration {
  private val service: SlackService = new Retrofit.Builder()
    .baseUrl("https://hooks.slack.com/")
    .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().setLenient().create()))
    .build()
    .create[SlackService](classOf[SlackService])
  private val statusTokenUrl: String = getTokenUrl("slack-status-token")
  private val activityTokenUrl: String = getTokenUrl("slack-status-activity-token")
  private val alertTokenUrl: String = getTokenUrl("slack-status-alerts-token")
  private val chatTokenUrl: String = getTokenUrl("slack-chat-token")

  def postMessageToSlack(message: String): Unit = {
    service.postMessage(statusTokenUrl, SlackMessage(message)).execute()
  }

  def postActivityMessageToSlack(message: String): Unit = {
    service.postMessage(activityTokenUrl, SlackMessage(message)).execute()
  }

  def postAlertToSlack(message: String): Unit = {
    service.postMessage(alertTokenUrl, SlackMessage(message)).execute()
  }

  def postRelayMessageToSlack(message: String): Unit = {
    service.postMessage(chatTokenUrl, SlackMessage(message)).execute()
  }

  private def getTokenUrl(tokenName: String): String = {
    val source = Source.fromFile(s"${System.getenv("HOME")}/.not-ogame-bots/$tokenName")
    val statusTokenUrl = source.getLines().toList.head
    source.close()
    statusTokenUrl
  }
}

trait SlackService {
  @POST
  def postMessage(@Url url: String, @Body sl: SlackMessage): Call[String]
}

case class SlackMessage(text: String)

object M {
  def main(args: Array[String]): Unit = {
    val integration = new SlackIntegration()
    integration.postActivityMessageToSlack("test")
    integration.postMessageToSlack("test")
    integration.postAlertToSlack("test")
  }
}
