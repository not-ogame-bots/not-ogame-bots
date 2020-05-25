package not.ogame.bots.ghostbuster

import cats.effect.Sync
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.reactive.Observable

trait FLogger { outer =>
  implicit def unsafeLogger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLoggerFromClass[F](outer.getClass)

  protected implicit val syncInstanceObservable: Sync[Observable] = new Observable.CatsInstances() with Sync[Observable] {
    override def suspend[A](thunk: => Observable[A]): Observable[A] = Observable.suspend(thunk)
  }
}
