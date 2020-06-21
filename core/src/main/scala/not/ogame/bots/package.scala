package not.ogame

import com.softwaremill.tagging._
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV

package object bots {
  sealed trait PlanetIdTag

  type PlanetId = String @@ PlanetIdTag

  object PlanetId {
    def apply(id: String): PlanetId = id.taggedWith[PlanetIdTag]
  }

  sealed trait FleetIdTag

  type FleetId = String @@ FleetIdTag

  object FleetId {
    def apply(id: String): FleetId = id.taggedWith[FleetIdTag]
  }

  case class AvailableDeuterExceeded(requiredAmount: Int)
      extends RuntimeException(s"There was not enough deuterium to send fleet, needed: $requiredAmount")

  def nonNegative(v: Int)(implicit ev: Validate[Int, NonNegative]): Refined[Int, NonNegative] =
    refineV[NonNegative](v).fold(s => throw new IllegalArgumentException(s), identity)
}
