package not.ogame.bots.selenium

import cats.effect.{ContextShift, IO, Timer}
import munit.FunSuite

abstract class CatsEffectSuite extends FunSuite {
  def munitContextShift: ContextShift[IO] =
    IO.contextShift(munitExecutionContext)

  implicit def munitTimer: Timer[IO] =
    IO.timer(munitExecutionContext)

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms ++ List(munitIOTransform)

  final def munitIOTransform: ValueTransform =
    new ValueTransform("IO", { case e: IO[_] => e.unsafeToFuture() })
}
