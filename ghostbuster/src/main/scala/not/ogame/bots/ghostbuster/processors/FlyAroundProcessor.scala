package not.ogame.bots.ghostbuster.processors

import not.ogame.bots.{CoordinatesType, Fleet, FleetAttitude, FleetMissionType, FleetResources, SendFleetRequest, SendFleetRequestShips}
//import not.ogame.bots.ghostbuster.{PlanetState, RandomTimeJitter, State, Action}
import com.softwaremill.quicklens._

//class FlyAroundProcessor(jitter: RandomTimeJitter) {
//  def apply(state: State.LoggedIn): State.LoggedIn = {
//    state.fleets
//      .find(
//        f =>
//          f.fleetAttitude == FleetAttitude.Friendly && f.to.coordinatesType == CoordinatesType.Planet && f.fleetMissionType == FleetMissionType.Deployment
//      )
//      .map { myFleet =>
//        state.modify(_.scheduledTasks).using(_ ++ singleFleet(myFleet, state.scheduledTasks, state.planets))
//      }
//      .getOrElse(state)
//  }
//
//  private def singleFleet(fleet: Fleet, tasks: List[Action], planets: List[PlanetState]): List[Action] = {
//    scheduleFleetReverse(fleet, tasks, planets) ++ scheduleRefreshBuildings(fleet, tasks, planets)
//  }
//
//  private def scheduleRefreshBuildings(fleet: Fleet, tasks: List[Action], planets: List[PlanetState]): List[Action] = {
//    val toPlanetId = planets.find(p => p.coords == fleet.to).get.id
//    if (!checkAlreadyInQueue(tasks) { case t: Action.RefreshSupplyAndFacilityPage if t.planetId == toPlanetId => t }) {
//      List(Action.RefreshSupplyAndFacilityPage(fleet.arrivalTime.plusSeconds(1), toPlanetId))
//    } else {
//      List.empty
//    }
//  }
//
//  private def scheduleFleetReverse(fleet: Fleet, tasks: List[Action], planets: List[PlanetState]) = {
//    if (!checkAlreadyInQueue(tasks) { case t: Action.SendFleet => t }) {
//      val fromPlanetId = planets.find(p => p.coords == fleet.to).get
//      List(
//        Action.SendFleet(
//          fleet.arrivalTime.plusSeconds(10 + jitter.getJitterInSeconds()),
//          SendFleetRequest(fromPlanetId.id, SendFleetRequestShips.AllShips, fleet.from, FleetMissionType.Deployment, FleetResources.Max)
//        )
//      )
//    } else {
//      List.empty
//    }
//  }
//}
