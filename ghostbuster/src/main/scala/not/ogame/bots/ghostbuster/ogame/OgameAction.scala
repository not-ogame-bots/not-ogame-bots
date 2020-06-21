package not.ogame.bots.ghostbuster.ogame

object OgameAction {
  def raiseError[A](t: Throwable): OgameAction[A] = cats.free.Free.liftF[OgameOp, A](OgameOp.RaiseError(t))
  def handleErrorWith[A](fa: OgameAction[A], f: Throwable => OgameAction[A]): OgameAction[A] =
    cats.free.Free.liftF[OgameOp, A](OgameOp.HandleError(fa, f))
}
