package not.ogame.bots.ghostbuster.actions

import not.ogame.bots.ghostbuster.actions.CollectResourcesAction.Request
import not.ogame.bots.{FleetMissionType, FleetResources, OgameDriver, PlanetId, SendFleetRequest, SendFleetRequestShips, ShipType}
import cats.implicits._
import monix.eval.Task
import not.ogame.bots.ghostbuster.executor._
import not.ogame.bots.ghostbuster.ogame.OgameAction

class CollectResourcesAction(taskExecutor: OgameDriver[OgameAction])(implicit executor: OgameActionExecutor[Task]) {
  def run(request: Request): Task[Unit] = {
    (for {
      planets <- taskExecutor.readPlanets()
      myFleetPage <- taskExecutor.readMyFleets()
      from = request.from.map(pId => planets.find(_.id == pId).get)
      to = planets.find(_.id == request.to).get
      freeSlots = myFleetPage.fleetSlots.maxFleets - myFleetPage.fleetSlots.currentFleets
      _ <- if (from.size <= freeSlots) {
        from.map { fromPlanet =>
          taskExecutor.sendFleet(
            SendFleetRequest(
              fromPlanet,
              SendFleetRequestShips.Ships(request.ships),
              to.coordinates,
              FleetMissionType.Transport,
              FleetResources.Max
            )
          )
        }.sequence
      } else {
        OgameAction.raiseError(new IllegalArgumentException(s"Not enough free fleet slots!! Requested ${from.size}, available $freeSlots"))
      }
    } yield ()).execute()
  }
}

object CollectResourcesAction {
  case class Request(from: List[PlanetId], to: PlanetId, ships: Map[ShipType, Int])
}
