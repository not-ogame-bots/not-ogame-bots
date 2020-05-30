package not.ogame

import com.softwaremill.tagging._

package object bots {
  sealed trait PlanetIdTag

  type PlanetId = String @@ PlanetIdTag

  object PlanetId {
    def apply(id: String): PlanetId = id.taggedWith[PlanetIdTag]
  }
}
