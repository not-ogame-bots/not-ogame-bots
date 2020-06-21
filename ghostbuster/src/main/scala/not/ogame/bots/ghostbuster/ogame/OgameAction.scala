package not.ogame.bots.ghostbuster.ogame

import cats.effect.ExitCase

object OgameAction {
  def raiseError[A](t: Throwable): OgameAction[A] = cats.free.Free.liftF[OgameOp, A](OgameOp.RaiseError(t))
  def handleErrorWith[A](fa: OgameAction[A], f: Throwable => OgameAction[A]): OgameAction[A] =
    cats.free.Free.liftF[OgameOp, A](OgameOp.HandleError(fa, f))
  def bracketCase[A, B](
      acquire: OgameAction[A]
  )(use: A => OgameAction[B])(release: (A, ExitCase[Throwable]) => OgameAction[Unit]): OgameAction[B] =
    cats.free.Free.liftF[OgameOp, B](OgameOp.BracketCase(acquire, use, release))
}
