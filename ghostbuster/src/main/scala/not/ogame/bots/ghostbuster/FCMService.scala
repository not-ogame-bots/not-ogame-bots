package not.ogame.bots.ghostbuster

import com.google.firebase.messaging.{AndroidConfig, AndroidNotification, ApnsConfig, Aps, FirebaseMessaging, Message, Notification}
import java.util.concurrent.{ExecutionException, TimeUnit}

import com.typesafe.scalalogging.StrictLogging
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.Duration

class FCMService extends StrictLogging {
  def sendMessage(data: Map[String, String], request: PushNotificationRequest): Unit = {
    val message = getPreconfiguredMessageWithData(data, request)
    val response = sendAndGetResponse(message)
    logger.info("Sent message with data. Topic: " + request.topic + ", " + response)
  }

  def sendMessageWithoutData(request: PushNotificationRequest): Unit = {
    val message = getPreconfiguredMessageWithoutData(request)
    val response = sendAndGetResponse(message)
    logger.info("Sent message without data. Topic: " + request.topic + ", " + response)
  }

  def sendMessageToToken(request: PushNotificationRequest): Unit = {
    val message = getPreconfiguredMessageToToken(request)
    val response = sendAndGetResponse(message)
    logger.info("Sent message to token. Device token: " + request.token + ", " + response)
  }

  private def sendAndGetResponse(message: Message) = FirebaseMessaging.getInstance.sendAsync(message).get

  private def getAndroidConfig(topic: String) =
    AndroidConfig.builder
      .setTtl(Duration(60, TimeUnit.MINUTES).toMillis)
      .setCollapseKey(topic)
      .setPriority(AndroidConfig.Priority.HIGH)
      .setNotification(
        AndroidNotification.builder
          .setSound("default")
          .setTag(topic)
          .build
      )
      .build

  private def getApnsConfig(topic: String) = ApnsConfig.builder.setAps(Aps.builder.setCategory(topic).setThreadId(topic).build).build

  private def getPreconfiguredMessageToToken(request: PushNotificationRequest) =
    getPreconfiguredMessageBuilder(request).setToken(request.token).build

  private def getPreconfiguredMessageWithoutData(request: PushNotificationRequest) =
    getPreconfiguredMessageBuilder(request).setTopic(request.topic).build

  private def getPreconfiguredMessageWithData(data: Map[String, String], request: PushNotificationRequest) =
    getPreconfiguredMessageBuilder(request).putAllData(data.asJava).setTopic(request.topic).build

  private def getPreconfiguredMessageBuilder(request: PushNotificationRequest) = {
    val androidConfig = getAndroidConfig(request.topic)
    val apnsConfig = getApnsConfig(request.topic)
    Message.builder
      .setApnsConfig(apnsConfig)
      .setAndroidConfig(androidConfig)
      .setNotification(new Notification(request.title, request.message))
  }
}
