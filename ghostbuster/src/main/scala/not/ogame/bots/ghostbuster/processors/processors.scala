package not.ogame.bots.ghostbuster

import java.time.Clock

import not.ogame.bots.ShipType
import not.ogame.bots.facts.ShipCosts

//package object processors {
//  private[processors] def checkAlreadyInQueue(tasks: List[Action])(p: PartialFunction[Action, Action]): Boolean = {
//    tasks.collectFirst(p).isDefined
//  }
//
//  private[processors] def buildingScheduled(tasks: List[Action], planetId: String): Boolean = {
//    tasks.collectFirst(buildingFilter(planetId)).isDefined
//  }
//
//  private[processors] def refreshScheduled(tasks: List[Action], planetId: String): Boolean = {
//    tasks.collectFirst { case t: Action.RefreshSupplyAndFacilityPage if t.planetId == planetId => t }.isDefined
//  }
//
//  private[processors] def refreshShipScheduled(tasks: List[Action], planetId: String, shipType: ShipType): Boolean = {
//    tasks.collectFirst { case t: Action.RefreshFleetOnPlanetStatus if t.planetId == planetId => t }.isDefined
//  }
//
//  private[processors] def noBuildingsInQueue(tasks: List[Action], planetId: String): Boolean = !buildingScheduled(tasks, planetId)
//
//  private[processors] def buildingFilter(planetId: String): PartialFunction[Action, Action] = {
//    case t: Action.BuildSupply if t.planetId == planetId   => t
//    case t: Action.BuildFacility if t.planetId == planetId => t
//    case t: Action.BuildShip if t.planetId == planetId     => t
//  }
//
//  def buildShip(planetState: PlanetState, ship: ShipType, jitterProvider: RandomTimeJitter, amount: Int, allowWaiting: Boolean = true)(
//      implicit clock: Clock
//  ): Option[Action.BuildShip] = { //TODO should check storages
//    val requiredResources = ShipCosts.shipCost(ship)
//    val currentResources = planetState.suppliesPage.currentResources
//    val currentProduction = planetState.suppliesPage.currentProduction
//    val canBuildAmount = Math.min(currentResources.div(requiredResources).map(_.toInt).min, amount)
//    if (canBuildAmount > 1) {
//      Some(Action.BuildShip(canBuildAmount, ship, clock.instant(), planetState.id))
//    } else if (allowWaiting) {
//      val stillNeed = requiredResources.difference(currentResources)
//      val hoursToWait = stillNeed.div(currentProduction).max
//      val secondsToWait = (hoursToWait * 3600).toInt + jitterProvider.getJitterInSeconds()
//      val timeOfExecution = clock.instant().plusSeconds(secondsToWait)
//      Some(Action.BuildShip(1, ship, timeOfExecution, planetId = planetState.id))
//    } else {
//      None
//    }
//  }
//}
