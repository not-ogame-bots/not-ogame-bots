package not.ogame.bots.ghostbuster.interpreter

trait TaskExecutor[F[_]] {
  def exec[T](action: F[T]): F[T]
}
