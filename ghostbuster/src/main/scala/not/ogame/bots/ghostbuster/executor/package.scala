package not.ogame.bots.ghostbuster
import cats.Monad
import cats.effect.{ExitCase, Sync}
import cats.free.Free
import not.ogame.bots.{MyFleet, OgameDriver, SendFleetRequest}
import not.ogame.bots.ghostbuster.ogame.{OgameAction, OgameOp}
import cats.implicits._

package object executor {
  implicit val ogameActionSyncInstance: Sync[OgameAction] = {
    val monad = Free.catsFreeMonadForFree[OgameOp]
    new Sync[OgameAction] {
      override def suspend[A](thunk: => OgameAction[A]): OgameAction[A] = monad.unit.flatMap(_ => thunk)
      override def bracketCase[A, B](acquire: OgameAction[A])(use: A => OgameAction[B])(
          release: (A, ExitCase[Throwable]) => OgameAction[Unit]
      ): OgameAction[B] = ???
      override def raiseError[A](e: Throwable): OgameAction[A] = OgameAction.raiseError(e)
      override def handleErrorWith[A](fa: OgameAction[A])(f: Throwable => OgameAction[A]): OgameAction[A] =
        OgameAction.handleErrorWith(fa, f)
      override def pure[A](x: A): OgameAction[A] = monad.pure(x)
      override def flatMap[A, B](fa: OgameAction[A])(f: A => OgameAction[B]): OgameAction[B] = monad.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => OgameAction[Either[A, B]]): OgameAction[B] = monad.tailRecM(a)(f)
    }
  }

  implicit class RichOgameAction[A](action: OgameAction[A]) {
    def execute[F[_]]()(implicit executor: OgameActionExecutor[F]): F[A] = {
      executor.execute(action)
    }
  }

  implicit class RichOGameDriver[F[_]: Monad](ogameDriver: OgameDriver[F]) {
    def sendAndTrackFleet(request: SendFleetRequest): F[MyFleet] = {
      for {
        myFleetsBefore <- ogameDriver.readMyFleets()
        _ <- ogameDriver.sendFleet(request)
        myFleetsAfter <- ogameDriver.readMyFleets()
      } yield {
        myFleetsAfter.fleets.filterNot(f => myFleetsBefore.fleets.map(_.fleetId).contains(f.fleetId)) match {
          case head :: Nil => head
          case other       => throw new IllegalStateException(s"Found more than one fleet: $other")
        }
      }
    }
  }
}
