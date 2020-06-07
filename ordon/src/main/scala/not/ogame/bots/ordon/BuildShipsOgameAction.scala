package not.ogame.bots.ordon

import cats.Monad
import cats.implicits._
import not.ogame.bots.ShipType.SmallCargoShip
import not.ogame.bots._
import not.ogame.bots.facts.ShipCosts

class BuildShipsOgameAction[T[_]: Monad](
    shipType: ShipType,
    number: Int,
    startPlanet: PlayerPlanet,
    allShipyardPlanets: List[PlayerPlanet]
)(implicit clock: LocalClock)
    extends OgameAction[T] {
  override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] = {
    oneAfterOther(sendResources(ogame)).flatMap(_ => createScheduledBuildActions(ogame))
  }

  private def sendResources(ogame: OgameDriver[T]): List[T[Unit]] = {
    val costPerPlanet = ShipCosts.shipCost(shipType) multiply (number / allShipyardPlanets.size)
    val unitsPerPlanet = costPerPlanet.metal + costPerPlanet.crystal + costPerPlanet.deuterium
    val numberOfSmallCargo = (unitsPerPlanet / 7000) + 1
    allShipyardPlanets
      .filter(_ != startPlanet)
      .map(
        to =>
          ogame.sendFleet(
            SendFleetRequest(
              from = startPlanet,
              ships = SendFleetRequestShips.Ships(Map((SmallCargoShip, numberOfSmallCargo))),
              targetCoordinates = to.coordinates,
              fleetMissionType = FleetMissionType.Transport,
              resources = FleetResources.Given(costPerPlanet),
              speed = FleetSpeed.Percent100
            )
          )
      )
  }

  private def oneAfterOther(actions: List[T[Unit]]): T[Unit] = {
    val unitT: T[Unit] = ().pure[T]
    actions.fold(unitT) { (a: T[Unit], b: T[Unit]) =>
      a.flatMap(_ => b)
    }
  }

  private def createScheduledBuildActions(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] = {
    ogame
      .readAllFleets()
      .map(fleets => fleets.filter(fleet => fleet.fleetMissionType == FleetMissionType.Transport && !fleet.isReturning))
      .map(fleets => fleets.map(fleet => createBuildAction(fleet)))
  }

  private def createBuildAction(fleet: Fleet): ScheduledAction[T] = {
    ScheduledAction(
      fleet.arrivalTime,
      new BuildShipsNowOgameAction[T](allShipyardPlanets.find(fleet.to == _.coordinates).get, shipType, number / allShipyardPlanets.size)
    )
  }
}

class BuildShipsNowOgameAction[T[_]: Monad](planet: PlayerPlanet, shipType: ShipType, count: Int)(implicit clock: LocalClock)
    extends OgameAction[T] {
  override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] = {
    ogame.buildShips(planet.id, shipType, count).map(_ => List[ScheduledAction[T]]())
  }
}
