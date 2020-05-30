package not.ogame.bots.ghostbuster.executor

import not.ogame.bots.{FacilityPageData, Fleet, PlayerPlanet, ShipType, SuppliesPageData}

trait StateChangeListener[F[_]] {
  def onNewSuppliesPage(planet: PlayerPlanet, suppliesPageData: SuppliesPageData): F[Unit]
  def onNewFacilitiesPage(planet: PlayerPlanet, facilityPageData: FacilityPageData): F[Unit]
  def onNewPlanetFleet(planet: PlayerPlanet, fleet: Map[ShipType, Int]): F[Unit]
  def onNewAirFleets(fleets: List[Fleet]): F[Unit]
  def onNewError(ex: Throwable): F[Unit]
}
