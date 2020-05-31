package not.ogame.bots.ghostbuster.infrastructure

case class PushNotificationRequest(title: String, message: String, topic: String, token: String, data: Map[String, String] = Map.empty)
