package not.ogame.bots.ordon

import cats.effect.IO
import cats.implicits._
import not.ogame.bots.CoordinatesType.Moon
import not.ogame.bots.FacilityBuilding.{NaniteFactory, ResearchLab, RoboticsFactory, Shipyard}
import not.ogame.bots.ShipType.{Destroyer, EspionageProbe, Explorer, LargeCargoShip}
import not.ogame.bots.SuppliesBuilding._
import not.ogame.bots._
import not.ogame.bots.ordon.action.ExpeditionOrdonAction

object OrdonQuasarConfig extends OrdonConfig {
  def getCredentials: Credentials = {
    val source = scala.io.Source.fromFile(s"${System.getenv("HOME")}/.not-ogame-bots/s169-pl.conf")
    val credentials = source
      .getLines()
      .toList
      .map(_.split(":")(1).trim.drop(1).dropRight(1))
    source.close()
    Credentials(credentials.head, credentials(1), credentials(2), credentials(3))
  }

  def getInitialActionsV2(): List[ExpeditionOrdonAction] = {
    val expeditionAction = new ExpeditionOrdonAction(moon, expeditionFleet)
    List(expeditionAction)
  }

  def getInitialActions(implicit clock: LocalClock): IO[List[ScheduledAction[IO]]] = {
    val listOfActions = List(
      new AlertOgameAction[IO](),
      new KeepActiveOgameAction[IO](List(planet, moon, planet2, planet3, planet4)),
      new FlyAroundWithLargeCargoOgameAction[IO](planet, moon),
      new DeployAndReturnNoLargeCargoOgameAction[IO](planet, moon),
      //      new BuildBuildingsOgameAction[IO](planet, taskQueue(clock)),
      //      new BuildBuildingsOgameAction[IO](planet2, taskQueue(clock)),
      //      new BuildBuildingsOgameAction[IO](planet3, taskQueue(clock)),
      //      new BuildBuildingsOgameAction[IO](planet4, taskQueue(clock)),
      new BuildBuildingsOgameAction[IO](
        planet5,
        List(
          new FacilityBuildingTask(RoboticsFactory, 10),
          new FacilityBuildingTask(NaniteFactory, 3),
          new FacilityBuildingTask(Shipyard, 12),
          new SuppliesBuildingTask(CrystalStorage, 10),
          new SuppliesBuildingTask(CrystalMine, 21),
          new SuppliesBuildingTask(CrystalMine, 22),
          new SuppliesBuildingTask(CrystalMine, 23),
          new SuppliesBuildingTask(CrystalMine, 24),
          new SuppliesBuildingTask(CrystalMine, 25),
          new SuppliesBuildingTask(CrystalMine, 26)
        )
      ),
      new BuildBuildingsOgameAction[IO](
        planet6,
        List(
          new FacilityBuildingTask(RoboticsFactory, 10),
          new FacilityBuildingTask(NaniteFactory, 3),
          new FacilityBuildingTask(Shipyard, 12),
          new SuppliesBuildingTask(CrystalStorage, 10),
          new SuppliesBuildingTask(CrystalMine, 15),
          new SuppliesBuildingTask(CrystalMine, 16),
          new SuppliesBuildingTask(CrystalMine, 17),
          new SuppliesBuildingTask(CrystalMine, 18),
          new SuppliesBuildingTask(CrystalMine, 19),
          new SuppliesBuildingTask(CrystalMine, 20),
          new SuppliesBuildingTask(CrystalMine, 21),
          new SuppliesBuildingTask(CrystalMine, 22),
          new SuppliesBuildingTask(CrystalMine, 23),
          new SuppliesBuildingTask(CrystalMine, 24),
          new SuppliesBuildingTask(CrystalMine, 25),
          new SuppliesBuildingTask(CrystalMine, 26)
        )
      ),
      new ExpeditionOgameAction[IO](
        moon,
        expeditionFleet,
        List(Coordinates(1, 155, 16))
      )
    )
    //    val coloniseAction = ScheduledAction(clock.now().withHour(3).withMinute(0).withSecond(0), new ColoniseAction[IO])
    IO.pure(listOfActions.map(ScheduledAction(clock.now(), _)))
  }

  private def taskQueue(implicit clock: LocalClock): List[TaskOnPlanet] = {
    List(
      new FacilityBuildingTask(RoboticsFactory, 10),
      new FacilityBuildingTask(Shipyard, 12),
      new FacilityBuildingTask(ResearchLab, 12),
      new SuppliesBuildingTask(MetalStorage, 11),
      new SuppliesBuildingTask(CrystalStorage, 10),
      new SuppliesBuildingTask(DeuteriumStorage, 8),
      //
      new SuppliesBuildingTask(MetalMine, 20),
      new SuppliesBuildingTask(CrystalMine, 20),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 20),
      new SuppliesBuildingTask(CrystalMine, 21),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 21),
      new SuppliesBuildingTask(CrystalMine, 22),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 22),
      new SuppliesBuildingTask(CrystalMine, 23),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 23),
      new SuppliesBuildingTask(CrystalMine, 24),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 24),
      new SuppliesBuildingTask(CrystalMine, 25),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 25),
      new SuppliesBuildingTask(CrystalMine, 26),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 26)
    )
  }

  private val expeditionFleet: Map[ShipType, Int] = Map(Destroyer -> 1, EspionageProbe -> 1, LargeCargoShip -> 400, Explorer -> 50)
  private val planet = PlayerPlanet(PlanetId.apply("33620959"), Coordinates(1, 155, 10))
  private val moon = PlayerPlanet(PlanetId.apply("33632870"), Coordinates(1, 155, 10, Moon))
  private val planet2 = PlayerPlanet(PlanetId.apply("33623552"), Coordinates(1, 155, 7))
  private val planet3 = PlayerPlanet(PlanetId.apply("33624816"), Coordinates(1, 154, 7))
  private val planet4 = PlayerPlanet(PlanetId.apply("33629451"), Coordinates(1, 155, 13))
  private val planet5 = PlayerPlanet(PlanetId.apply("33635176"), Coordinates(1, 155, 6))
  private val planet6 = PlayerPlanet(PlanetId.apply("33635734"), Coordinates(1, 155, 3))
}
