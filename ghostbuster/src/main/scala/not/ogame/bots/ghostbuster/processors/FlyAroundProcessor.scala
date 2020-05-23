package not.ogame.bots.ghostbuster.processors

import java.time.ZoneOffset

import not.ogame.bots.{Fleet, FleetAttitude, FleetMissionType, FleetResources, SendFleetRequest, SendFleetRequestShips}
import not.ogame.bots.ghostbuster.{PlanetState, RandomTimeJitter, State, Task}
import com.softwaremill.quicklens._

class FlyAroundProcessor(jitter: RandomTimeJitter) {
  def apply(state: State.LoggedIn): State.LoggedIn = {
    val myFleet = state.fleets.find(_.fleetAttitude == FleetAttitude.Friendly).head
    state.modify(_.scheduledTasks).using(_ ++ singleFleet(myFleet, state.scheduledTasks, state.planets))
  }

  def singleFleet(fleet: Fleet, tasks: List[Task], planets: List[PlanetState]): List[Task] = {
    if (!checkAlreadyInQueue(tasks) { case t: Task.SendFleet => t }) {
      val fromPlanetId = planets.find(p => p.coords == fleet.to).get
      List(
        Task.SendFleet(
          fleet.arrivalTime.toInstant(ZoneOffset.ofHours(2)).plusSeconds(10 + jitter.getJitterInSeconds()),
          SendFleetRequest(fromPlanetId.id, SendFleetRequestShips.AllShips, fleet.from, FleetMissionType.Deployment, FleetResources.Max)
        )
      )
    } else {
      List.empty
    }
  }
}
