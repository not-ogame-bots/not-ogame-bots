package not.ogame.bots.ghostbuster.reporting

import cats.effect.concurrent.Ref
import cats.implicits._
import com.softwaremill.quicklens._
import monix.eval.Task
import monix.reactive.Consumer
import not.ogame.bots._
import not.ogame.bots.ghostbuster.executor.Notification
import not.ogame.bots.ghostbuster.processors.TaskExecutor

class StateAggregator(state: Ref[Task, State], taskExecutor: TaskExecutor)(implicit clock: LocalClock) {
  def run(): Task[Unit] = {
    taskExecutor.subscribeToNotifications.consumeWith(Consumer.foreachTask {
      case Notification.SuppliesPageDateRefreshed(value, playerPlanet) => onNewSuppliesPage(playerPlanet, value)
      case Notification.FacilityPageDataRefreshed(value, playerPlanet) => onNewFacilitiesPage(playerPlanet, value)
      case Notification.Failure(ex)                                    => onNewError(ex)
      case Notification.FleetOnPlanetRefreshed(value)                  => onNewPlanetFleet(value.playerPlanet, value.fleet)
      case Notification.GetAirFleet(value)                             => onNewAirFleets(value)
      case _                                                           => Task.unit
    })
  }

  def onNewSuppliesPage(planet: PlayerPlanet, suppliesPageData: SuppliesPageData): Task[Unit] = {
    state.update { s =>
      val currentPlanetState = s.planets.getOrElse(planet.coordinates, PlanetState.Empty)
      val newPlanetState = currentPlanetState
        .modify(_.currentBuildingProgress)
        .setTo(suppliesPageData.currentBuildingProgress)
        .modify(_.currentShipyardProgress)
        .setTo(suppliesPageData.currentShipyardProgress)
        .modify(_.currentResources)
        .setTo(Some(suppliesPageData.currentResources))
        .modify(_.currentProduction)
        .setTo(Some(suppliesPageData.currentProduction))
        .modify(_.currentCapacity)
        .setTo(Some(suppliesPageData.currentCapacity))
        .modify(_.suppliesLevels)
        .setTo(Some(suppliesPageData.suppliesLevels))
      val updatedState = s
        .modify(_.planets)
        .using(_ ++ Map(planet.coordinates -> newPlanetState))
        .modify(_.lastTimestamp)
        .setTo(Some(clock.now()))
      updatedState
        .modify(_.summaryResourcesOnPlanets)
        .setTo(Some(updatedState.planets.values.flatMap(_.currentResources).reduce(_ add _)))
    }
  }

  def onNewFacilitiesPage(planet: PlayerPlanet, facilityPageData: FacilityPageData): Task[Unit] = {
    state.update { s =>
      val currentPlanetState = s.planets.getOrElse(planet.coordinates, PlanetState.Empty)
      val newPlanetState = currentPlanetState
        .modify(_.currentBuildingProgress)
        .setTo(facilityPageData.currentBuildingProgress)
        .modify(_.currentResources)
        .setTo(Some(facilityPageData.currentResources))
        .modify(_.currentProduction)
        .setTo(Some(facilityPageData.currentProduction))
        .modify(_.currentCapacity)
        .setTo(Some(facilityPageData.currentCapacity))
        .modify(_.facilitiesBuildingLevels)
        .setTo(Some(facilityPageData.facilityLevels))
      val updatedState = s
        .modify(_.planets)
        .using(_ ++ Map(planet.coordinates -> newPlanetState))
        .modify(_.lastTimestamp)
        .setTo(Some(clock.now()))
      updatedState
        .modify(_.summaryResourcesOnPlanets)
        .setTo(Some(updatedState.planets.values.flatMap(_.currentResources).reduce(_ add _)))
    }
  }

  def onNewPlanetFleet(planet: PlayerPlanet, fleet: Map[ShipType, Int]): Task[Unit] = {
    state.update { s =>
      val currentPlanetState = s.planets.getOrElse(planet.coordinates, PlanetState.Empty)
      val newPlanetState = currentPlanetState
        .modify(_.fleet)
        .setTo(Some(fleet))
      val updatedState = s
        .modify(_.planets)
        .using(_ ++ Map(planet.coordinates -> newPlanetState))
        .modify(_.lastTimestamp)
        .setTo(Some(clock.now()))

      updatedState
        .modify(_.summaryFleetOnPlanets)
        .setTo(updatedState.planets.values.flatMap(_.fleet).foldLeft(Map.empty[ShipType, Int])(_ |+| _))
    }
  }

  def onNewAirFleets(fleets: List[Fleet]): Task[Unit] = {
    state.update(
      _.modify(_.airFleets)
        .setTo(fleets)
        .modify(_.lastTimestamp)
        .setTo(Some(clock.now()))
        .modify(_.enemyFleets)
        .setTo(fleets.filter(f => f.fleetAttitude == FleetAttitude.Hostile))
    )
  }
  def onNewError(ex: Throwable): Task[Unit] = {
    state.update(_.copy(lastError = Some(clock.now() -> ex.getMessage)))
  }
}
