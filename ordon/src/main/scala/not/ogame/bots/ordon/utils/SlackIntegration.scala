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
  private val statusTokenUrl: String = getStatusTokenUrl

  def postMessageToSlack(message: String): Unit = {
    service.postMessage(statusTokenUrl, SlackMessage(message)).execute()
  }

  private def getStatusTokenUrl: String = {
    val source = Source.fromFile(s"${System.getenv("HOME")}/.not-ogame-bots/slack-status-token")
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
