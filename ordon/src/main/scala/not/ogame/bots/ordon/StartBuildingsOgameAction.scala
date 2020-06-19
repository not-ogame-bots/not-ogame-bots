package not.ogame.bots.ordon

import cats.Monad
import cats.implicits._
import not.ogame.bots.FacilityBuilding.{ResearchLab, RoboticsFactory, Shipyard}
import not.ogame.bots.SuppliesBuilding.{CrystalMine, DeuteriumSynthesizer, MetalMine}
import not.ogame.bots.Technology._
import not.ogame.bots.{LocalClock, OgameDriver}

class StartBuildingsOgameAction[T[_]: Monad](implicit clock: LocalClock) extends OgameAction[T] {
  override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] =
    for {
      planets <- ogame.readPlanets()
    } yield List(ScheduledAction(clock.now(), new BuildBuildingsOgameAction[T](planets.head, taskQueue())))

  private def taskQueue(): List[TaskOnPlanet] = {
    List(
      new SuppliesBuildingTask(CrystalMine, 5),
      new SuppliesBuildingTask(MetalMine, 7),
      new SuppliesBuildingTask(CrystalMine, 6),
      new SuppliesBuildingTask(MetalMine, 8),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 4),
      new SuppliesBuildingTask(CrystalMine, 7),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 5),
      new SuppliesBuildingTask(CrystalMine, 8),
      new FacilityBuildingTask(RoboticsFactory, 1),
      new FacilityBuildingTask(RoboticsFactory, 2),
      new FacilityBuildingTask(Shipyard, 1),
      new FacilityBuildingTask(ResearchLab, 1),
      new FacilityBuildingTask(ResearchLab, 2),
      new FacilityBuildingTask(ResearchLab, 3),
      new TechnologyBuildingTask(Energy, 1),
      new TechnologyBuildingTask(CombustionDrive, 1),
      new TechnologyBuildingTask(CombustionDrive, 2),
      new TechnologyBuildingTask(Espionage, 1),
      new TechnologyBuildingTask(Espionage, 2),
      new TechnologyBuildingTask(Espionage, 3),
      new TechnologyBuildingTask(Espionage, 4),
      new TechnologyBuildingTask(ImpulseDrive, 1),
      new FacilityBuildingTask(Shipyard, 2),
      new FacilityBuildingTask(Shipyard, 3),
      new TechnologyBuildingTask(ImpulseDrive, 2),
      new FacilityBuildingTask(Shipyard, 4),
      new TechnologyBuildingTask(ImpulseDrive, 3),
      new TechnologyBuildingTask(Astrophysics, 1),
      new TechnologyBuildingTask(Computer, 1),
      new TechnologyBuildingTask(Computer, 2),
      new TechnologyBuildingTask(Computer, 3),
      new TechnologyBuildingTask(Computer, 4)
    )
  }
}
