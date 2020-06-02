package not.ogame.bots.ghostbuster.infrastructure

import cats.effect.Resource
import com.google.firebase.FirebaseApp
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task

object FirebaseResource extends StrictLogging {
  def create(settingsDirectory: String): Resource[Task, FirebaseApp] = {
    Resource.make(Task.eval(initializeFirebase(settingsDirectory)))(r => Task.eval(r.delete()))
  }

  private def initializeFirebase(settingsDirectory: String) = {
    import com.google.auth.oauth2.GoogleCredentials
    import com.google.firebase.FirebaseApp
    import com.google.firebase.FirebaseOptions
    import java.io.FileInputStream
    val serviceAccount = new FileInputStream(s"$settingsDirectory/notogamebots-firebase-adminsdk-sjbas-730cc8387b.json")

    val options = new FirebaseOptions.Builder()
      .setCredentials(GoogleCredentials.fromStream(serviceAccount))
      .setDatabaseUrl("https://notogamebots.firebaseio.com")
      .build

    val app = FirebaseApp.initializeApp(options)
    logger.info("Firebase initialized")
    app
  }
}
