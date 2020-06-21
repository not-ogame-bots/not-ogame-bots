package not.ogame.bots.ordon

import cats.Monad
import cats.implicits._
import not.ogame.bots.FacilityBuilding.{RoboticsFactory, Shipyard}
import not.ogame.bots.ShipType._
import not.ogame.bots.SuppliesBuilding.{CrystalMine, DeuteriumSynthesizer, MetalMine}
import not.ogame.bots.{Coordinates, LocalClock, OgameDriver, PlayerPlanet}

class StartOgameAction[T[_]: Monad](implicit clock: LocalClock) extends OgameAction[T] {
  override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] =
    for {
      planets <- ogame.readPlanets()
      buildActions = planets.map(planet => new BuildBuildingsOgameAction[T](planet, taskQueue()))
    } yield (buildActions ++ List(expeditionAction(planets))).map(action => ScheduledAction(clock.now(), action))

  private def expeditionAction(planets: List[PlayerPlanet]): ExpeditionOgameAction[T] = {
    new ExpeditionOgameAction[T](
      maxNumberOfExpeditions = 4,
      planets.head,
      Map(SmallCargoShip -> 11, LargeCargoShip -> 5, Cruiser -> 1, LightFighter -> 1, EspionageProbe -> 1, Explorer -> 2),
      List(Coordinates(1, 155, 16))
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
      new SuppliesBuildingTask(MetalMine, 16)
    )
  }
}
