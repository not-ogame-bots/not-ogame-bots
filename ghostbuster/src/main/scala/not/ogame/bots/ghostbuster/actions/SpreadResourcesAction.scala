package not.ogame.bots.ghostbuster.actions

import cats.implicits._
import monix.eval.Task
import not.ogame.bots.ghostbuster.executor._
import not.ogame.bots.ghostbuster.ogame.OgameAction
import not.ogame.bots._
import not.ogame.bots.ghostbuster.actions.SpreadResourcesAction.Request

class SpreadResourcesAction(driver: OgameDriver[OgameAction])(implicit executor: OgameActionExecutor[Task]) {
  def run(request: Request): Task[Unit] = {
    (for {
      planets <- driver.readPlanets()
      myFleetPage <- driver.readMyFleets()
      to = request.to.map(pId => planets.find(_.id == pId).get)
      from = planets.find(_.id == request.from).get
      freeSlots = myFleetPage.fleetSlots.maxFleets - myFleetPage.fleetSlots.currentFleets
      _ <- if (to.size <= freeSlots) {
        to.map { toPlanet =>
          driver.sendFleet(
            SendFleetRequest(
              from,
              SendFleetRequestShips.Ships(request.ships),
              toPlanet.coordinates,
              request.missionType,
              FleetResources.Given(request.resources)
            )
          )
        }.sequence
      } else {
        OgameAction.raiseError(new IllegalArgumentException(s"Not enough free fleet slots!! Requested ${to.size}, available $freeSlots"))
      }
    } yield ()).execute()
  }
}

object SpreadResourcesAction {
  case class Request(
      from: PlanetId,
      to: List[PlanetId],
      ships: Map[ShipType, Int],
      resources: Resources,
      missionType: FleetMissionType = FleetMissionType.Deployment
  )
}
