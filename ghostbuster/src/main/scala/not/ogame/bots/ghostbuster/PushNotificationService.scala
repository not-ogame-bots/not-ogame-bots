package not.ogame.bots.ghostbuster

import com.google.api.client.util.Value
import com.sun.org.slf4j.internal.LoggerFactory
import java.util.concurrent.ExecutionException

import com.typesafe.scalalogging.StrictLogging

import scala.collection.mutable

class PushNotificationService(var fcmService: FCMService) extends StrictLogging {
  def sendPushNotification(request: PushNotificationRequest): Unit = {
    try fcmService.sendMessage(getSamplePayloadData, request)
    catch {
      case e @ (_: InterruptedException | _: ExecutionException) =>
        logger.error(e.getMessage)
    }
  }

  def sendPushNotificationWithoutData(request: PushNotificationRequest): Unit = {
    try fcmService.sendMessageWithoutData(request)
    catch {
      case e @ (_: InterruptedException | _: ExecutionException) =>
        logger.error(e.getMessage)
    }
  }

  def sendPushNotificationToToken(request: Nothing): Unit = {
    try fcmService.sendMessageToToken(request)
    catch {
      case e @ (_: InterruptedException | _: ExecutionException) =>
        logger.error(e.getMessage)
    }
  }

  private def getSamplePayloadData = {
    val pushData = new mutable.HashMap[String, String]()
    pushData.put("messageId", "messageId")
    pushData.put("text", "data goes here")
    pushData.toMap
  }
}
