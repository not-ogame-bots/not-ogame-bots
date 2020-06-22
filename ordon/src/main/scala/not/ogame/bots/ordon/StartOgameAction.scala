package not.ogame.bots.ordon

import cats.Monad
import cats.implicits._
import not.ogame.bots.FacilityBuilding.{RoboticsFactory, Shipyard}
import not.ogame.bots.ShipType._
import not.ogame.bots.SuppliesBuilding.{CrystalMine, DeuteriumSynthesizer, MetalMine}
import not.ogame.bots._
import not.ogame.bots.ordon.utils.{FleetSelector, Selector}

class StartOgameAction[T[_]: Monad](implicit clock: LocalClock) extends OgameAction[T] {
  override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] =
    for {
      planets <- ogame.readPlanets()
      buildActions = planets.map(planet => new BuildBuildingsOgameAction[T](planet, taskQueue()))
      otherActions = List(expeditionAction(planets), flyAround(planets))
    } yield {
      (buildActions ++ otherActions).map(action => ScheduledAction(clock.now(), action))
    }

  private def expeditionAction(planets: List[PlayerPlanet]): OgameAction[T] = {
    new BalancingExpeditionOgameAction[T](
      planets.head,
      List(Coordinates(1, 155, 16))
    )
  }

  private def flyAround(planets: List[PlayerPlanet]): OgameAction[T] = {
    val leaveExpeditionShips = new FleetSelector(
      Map(
        SmallCargoShip -> Selector.skip,
        LargeCargoShip -> Selector.skip,
        Battleship -> Selector.decreaseBy(3),
        Destroyer -> Selector.skip,
        Explorer -> Selector.skip,
        EspionageProbe -> Selector.decreaseBy(50)
      )
    )
    new FlyAroundOgameAction[T](
      speed = FleetSpeed.Percent10,
      targets = planets.take(2),
      fleetSelector = p => if (p == planets.head) leaveExpeditionShips else new FleetSelector(),
      resourceSelector = _ => { _ => Resources(0, 0, 5_000) }
    )
  }

  private def taskQueue(): List[TaskOnPlanet] = {
    List(
      new SuppliesBuildingTask(MetalMine, 1),
      new SuppliesBuildingTask(MetalMine, 2),
      new SuppliesBuildingTask(MetalMine, 3),
      new SuppliesBuildingTask(MetalMine, 4),
      new SuppliesBuildingTask(MetalMine, 5),
      new SuppliesBuildingTask(CrystalMine, 1),
      new SuppliesBuildingTask(CrystalMine, 2),
      new SuppliesBuildingTask(CrystalMine, 3),
      new SuppliesBuildingTask(CrystalMine, 4),
      new SuppliesBuildingTask(MetalMine, 6),
      new SuppliesBuildingTask(CrystalMine, 5),
      new SuppliesBuildingTask(MetalMine, 7),
      new SuppliesBuildingTask(CrystalMine, 6),
      new SuppliesBuildingTask(MetalMine, 8),
      new SuppliesBuildingTask(CrystalMine, 7),
      new SuppliesBuildingTask(CrystalMine, 8),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 1),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 2),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 3),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 4),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 5),
      new FacilityBuildingTask(RoboticsFactory, 1),
      new FacilityBuildingTask(RoboticsFactory, 2),
      new FacilityBuildingTask(Shipyard, 1),
      new SuppliesBuildingTask(CrystalMine, 9),
      new SuppliesBuildingTask(MetalMine, 9),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 6),
      new SuppliesBuildingTask(CrystalMine, 10),
      new SuppliesBuildingTask(MetalMine, 10),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 7),
      new SuppliesBuildingTask(CrystalMine, 11),
      new SuppliesBuildingTask(MetalMine, 11),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 8),
      new SuppliesBuildingTask(CrystalMine, 12),
      new SuppliesBuildingTask(MetalMine, 12),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 9),
      new SuppliesBuildingTask(CrystalMine, 13),
      new SuppliesBuildingTask(MetalMine, 13),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 10),
      new SuppliesBuildingTask(CrystalMine, 14),
      new SuppliesBuildingTask(MetalMine, 14),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 11),
      new SuppliesBuildingTask(CrystalMine, 15),
      new SuppliesBuildingTask(MetalMine, 15),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 12),
      new SuppliesBuildingTask(CrystalMine, 16),
      new SuppliesBuildingTask(MetalMine, 16),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 13),
      new SuppliesBuildingTask(CrystalMine, 17),
      new SuppliesBuildingTask(MetalMine, 17),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 14),
      new SuppliesBuildingTask(CrystalMine, 18),
      new SuppliesBuildingTask(MetalMine, 18),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 15),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 16),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 17),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 18),
      new SuppliesBuildingTask(CrystalMine, 19),
      new SuppliesBuildingTask(CrystalMine, 20),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 19),
      new SuppliesBuildingTask(CrystalMine, 21),
      new SuppliesBuildingTask(MetalMine, 19),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 20),
      new SuppliesBuildingTask(CrystalMine, 22),
      new SuppliesBuildingTask(MetalMine, 20),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 21),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 22)
    )
  }
}
