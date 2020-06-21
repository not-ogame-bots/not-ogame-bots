package not.ogame.bots
import eu.timepit.refined._
import eu.timepit.refined.api.{Refined, Validate}

package object selenium {
  @Deprecated
  def refineVUnsafe[P, V](v: V)(implicit ev: Validate[V, P]): Refined[V, P] =
    refineV[P](v).fold(s => throw new IllegalArgumentException(s), identity)
}
