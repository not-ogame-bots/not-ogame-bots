package not.ogame.bots.ghostbuster.reporting

import cats.effect.Sync
import not.ogame.bots.{FacilityPageData, Fleet, PlayerPlanet, ShipType, SuppliesPageData}
import not.ogame.bots.ghostbuster.executor.StateChangeListener
import cats.implicits._

class StateListenerDispatcher[F[_]: Sync](listeners: List[StateChangeListener[F]]) extends StateChangeListener[F] {
  override def onNewSuppliesPage(planet: PlayerPlanet, suppliesPageData: SuppliesPageData): F[Unit] = {
    listeners.traverse(_.onNewSuppliesPage(planet, suppliesPageData)).void
  }

  override def onNewFacilitiesPage(planet: PlayerPlanet, facilityPageData: FacilityPageData): F[Unit] = {
    listeners.traverse(_.onNewFacilitiesPage(planet, facilityPageData)).void
  }

  override def onNewPlanetFleet(planet: PlayerPlanet, fleet: Map[ShipType, Int]): F[Unit] = {
    listeners.traverse(_.onNewPlanetFleet(planet, fleet)).void
  }

  override def onNewAirFleets(fleets: List[Fleet]): F[Unit] = {
    listeners.traverse(_.onNewAirFleets(fleets)).void
  }

  override def onNewError(ex: Throwable): F[Unit] = {
    listeners.traverse(_.onNewError(ex)).void
  }
}
