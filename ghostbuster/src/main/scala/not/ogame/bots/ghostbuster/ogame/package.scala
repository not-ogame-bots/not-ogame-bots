package not.ogame.bots.ghostbuster
import acyclic.skipped
import cats.free.Free

package object ogame {
  type OgameAction[A] = Free[OgameOp, A]
}
