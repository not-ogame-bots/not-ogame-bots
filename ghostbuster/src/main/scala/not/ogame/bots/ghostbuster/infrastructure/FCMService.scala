package not.ogame.bots.ghostbuster.infrastructure

import java.util.concurrent.TimeUnit

import cats.effect.Sync
import com.google.firebase.messaging._
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

class FCMService[F[_]: Sync](settingsDirectory: String) extends StrictLogging {
  def initializeFirebase(): Unit = {
    import com.google.auth.oauth2.GoogleCredentials
    import com.google.firebase.FirebaseApp
    import com.google.firebase.FirebaseOptions
    import java.io.FileInputStream
    val serviceAccount = new FileInputStream(s"${settingsDirectory}/notogamebots-firebase-adminsdk-sjbas-730cc8387b.json")

    val options = new FirebaseOptions.Builder()
      .setCredentials(GoogleCredentials.fromStream(serviceAccount))
      .setDatabaseUrl("https://notogamebots.firebaseio.com")
      .build

    FirebaseApp.initializeApp(options)
    logger.info("Firebase initialized")
  }

  def sendMessage(data: Map[String, String], request: PushNotificationRequest): F[Unit] = Sync[F].delay {
    val message = getPreconfiguredMessageWithData(data, request)
    val response = sendAndGetResponse(message)
    logger.info("Sent message with data. Topic: " + request.topic + ", " + response)
  }

  def sendMessageWithoutData(request: PushNotificationRequest): F[Unit] = Sync[F].delay {
    val message = getPreconfiguredMessageWithoutData(request)
    val response = sendAndGetResponse(message)
    logger.info("Sent message without data. Topic: " + request.topic + ", " + response)
  }

  def sendMessageToToken(request: PushNotificationRequest): F[Unit] = Sync[F].delay {
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
