package not.ogame.bots.ghostbuster.reporting

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import not.ogame.bots.ghostbuster.FLogger
import not.ogame.bots.ghostbuster.executor.EmptyStateChangeListener
import not.ogame.bots.ghostbuster.infrastructure.{FCMService, PushNotificationRequest}
import not.ogame.bots.{Fleet, FleetAttitude}

class HostileFleetReporter[F[_]: Sync](fcmService: FCMService[F], seenFleets: Ref[F, Set[Fleet]])
    extends EmptyStateChangeListener[F]
    with FLogger {
  override def onNewAirFleets(fleets: List[Fleet]): F[Unit] = {
    seenFleets.get.flatMap { alreadySeen =>
      fleets.filter(f => f.fleetAttitude == FleetAttitude.Hostile && !alreadySeen.contains(f)) match {
        case fleets if fleets.nonEmpty =>
          Logger[F].warn("!!!! HOSTILE FLEET DETECTED !!!!") >>
            fcmService.sendMessageWithoutData(
              PushNotificationRequest("Attention", "My Imperator, a hostile fleet has been detected", "attacks", null)
            ) >> seenFleets.update(_ ++ fleets)
        case Nil => Sync[F].unit
      }
    }
  }
}
