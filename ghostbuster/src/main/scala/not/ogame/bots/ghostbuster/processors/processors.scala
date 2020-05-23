package not.ogame.bots.ghostbuster

import not.ogame.bots.ShipType

package object processors {
  private[processors] def checkAlreadyInQueue(tasks: List[Task])(p: PartialFunction[Task, Task]): Boolean = {
    tasks.collectFirst(p).isDefined
  }

  private[processors] def buildingScheduled(tasks: List[Task], planetId: String): Boolean = {
    tasks.collectFirst(buildingFilter(planetId)).isDefined
  }

  private[processors] def refreshScheduled(tasks: List[Task], planetId: String): Boolean = {
    tasks.collectFirst { case t: Task.RefreshSupplyAndFacilityPage if t.planetId == planetId => t }.isDefined
  }

  private[processors] def refreshShipScheduled(tasks: List[Task], planetId: String, shipType: ShipType): Boolean = {
    tasks.collectFirst { case t: Task.RefreshFleetOnPlanetStatus if t.planetId == planetId && t.shipType == shipType => t }.isDefined
  }

  private[processors] def noBuildingsInQueue(tasks: List[Task], planetId: String): Boolean = !buildingScheduled(tasks, planetId)

  private[processors] def buildingFilter(planetId: String): PartialFunction[Task, Task] = {
    case t: Task.BuildSupply if t.planetId == planetId   => t
    case t: Task.BuildFacility if t.planetId == planetId => t
    case t: Task.BuildShip if t.planetId == planetId     => t
  }
}
