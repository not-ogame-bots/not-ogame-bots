package not.ogame

import com.softwaremill.tagging._

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

  class AvailableDeuterExceeded(requiredAmount: String)
      extends RuntimeException(s"There was not enough deuterium to send fleet, needed: $requiredAmount")
}
