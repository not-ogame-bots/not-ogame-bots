package not.ogame.bots.ghostbuster.executor

import cats.effect.Sync
import not.ogame.bots._

trait StateChangeListener[F[_]] {
  def onNewSuppliesPage(planet: PlayerPlanet, suppliesPageData: SuppliesPageData): F[Unit]
  def onNewFacilitiesPage(planet: PlayerPlanet, facilityPageData: FacilityPageData): F[Unit]
  def onNewPlanetFleet(planet: PlayerPlanet, fleet: Map[ShipType, Int]): F[Unit]
  def onNewAirFleets(fleets: List[Fleet]): F[Unit]
  def onNewError(ex: Throwable): F[Unit]
}

class EmptyStateChangeListener[F[_]: Sync] extends StateChangeListener[F] {
  override def onNewSuppliesPage(planet: PlayerPlanet, suppliesPageData: SuppliesPageData): F[Unit] = Sync[F].unit
  override def onNewFacilitiesPage(planet: PlayerPlanet, facilityPageData: FacilityPageData): F[Unit] = Sync[F].unit
  override def onNewPlanetFleet(planet: PlayerPlanet, fleet: Map[ShipType, Int]): F[Unit] = Sync[F].unit
  override def onNewAirFleets(fleets: List[Fleet]): F[Unit] = Sync[F].unit
  override def onNewError(ex: Throwable): F[Unit] = Sync[F].unit
}
