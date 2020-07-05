package not.ogame.bots.ordon

import cats.effect.IO
import cats.implicits._
import not.ogame.bots.CoordinatesType.Moon
import not.ogame.bots.FacilityBuilding.{NaniteFactory, ResearchLab, RoboticsFactory, Shipyard}
import not.ogame.bots.ShipType._
import not.ogame.bots.SuppliesBuilding._
import not.ogame.bots.Technology._
import not.ogame.bots._
import not.ogame.bots.ordon.action._
import not.ogame.bots.ordon.core.OrdonAction

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

  def getInitialActions(implicit clock: LocalClock): IO[List[ScheduledAction[IO]]] = {
    val listOfActions = List(
      new AlertOgameAction[IO](),
      new KeepActiveOgameAction[IO](List(planet10, moon10, planet7, planet7_154, planet13)),
      //      new FlyAroundWithLargeCargoOgameAction[IO](planet, moon),
      //      new DeployAndReturnNoLargeCargoOgameAction[IO](planet, moon),
      //      new BuildBuildingsOgameAction[IO](planet, taskQueue(clock)),
      //      new BuildBuildingsOgameAction[IO](planet2, taskQueue(clock)),
      //      new BuildBuildingsOgameAction[IO](planet3, taskQueue(clock)),
      //      new BuildBuildingsOgameAction[IO](planet4, taskQueue(clock)),
      //      new BuildBuildingsOgameAction[IO](planet5, newPlanetBuildingList),
      //      new BuildBuildingsOgameAction[IO](planet6, newPlanetBuildingList),
      new ExpeditionOgameAction[IO](moon10, expeditionFleet, List(Coordinates(1, 155, 16)))
    )
    //    val coloniseAction = ScheduledAction(clock.now().withHour(3).withMinute(0).withSecond(0), new ColoniseAction[IO])
    IO.pure(listOfActions.map(ScheduledAction(clock.now(), _)))
  }

  private def newPlanetBuildingList(implicit clock: LocalClock): List[TaskOnPlanet] = {
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
      new SuppliesBuildingTask(CrystalMine, 26),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 10),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 15),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 17),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 19),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 20),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 21),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 22),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 23),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 24),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 25),
      new SuppliesBuildingTask(DeuteriumSynthesizer, 26),
      new SuppliesBuildingTask(MetalMine, 20)
    )
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

  def monitorSos(): OrdonAction =
    new MonitorActivityOrdonAction(
      "SOS",
      planet10,
      List(
        Coordinates(1, 154, 1),
        Coordinates(1, 154, 2),
        Coordinates(1, 154, 9),
        Coordinates(1, 154, 9, Moon),
        Coordinates(1, 154, 10),
        Coordinates(1, 154, 10, Moon),
        Coordinates(1, 154, 15),
        Coordinates(1, 154, 15, Moon),
        Coordinates(1, 168, 1)
      )
    )

  def monitorRadamantys(): OrdonAction = {
    new MonitorActivityOrdonAction(
      "Radamantys",
      planet10,
      List(
        Coordinates(1, 140, 5),
        Coordinates(1, 140, 6),
        Coordinates(1, 140, 7),
        Coordinates(1, 140, 11),
        Coordinates(6, 126, 7),
        Coordinates(6, 126, 8)
      )
    )
  }

  def monitorVed(): OrdonAction = {
    new MonitorActivityOrdonAction(
      "Ved",
      planet10,
      List(
        Coordinates(1, 138, 6),
        Coordinates(1, 138, 6, Moon),
        Coordinates(1, 138, 7),
        Coordinates(1, 139, 7),
        Coordinates(1, 226, 15),
        Coordinates(1, 496, 13)
      )
    )
  }

  def monitorAdmiralSun(): OrdonAction = {
    new MonitorActivityOrdonAction(
      "Admiral Sun",
      planet10,
      List(
        Coordinates(1, 142, 9),
        Coordinates(1, 143, 6),
        Coordinates(1, 143, 6, Moon),
        Coordinates(2, 233, 8),
        Coordinates(2, 233, 9),
        Coordinates(3, 305, 15)
      )
    )
  }

  def monitorAdmiralMagnetar(): OrdonAction = {
    new MonitorActivityOrdonAction(
      "Admiral Magnetar",
      planet10,
      List(
        Coordinates(1, 124, 12),
        Coordinates(1, 124, 12, Moon),
        Coordinates(1, 124, 13),
        Coordinates(1, 127, 5),
        Coordinates(1, 327, 11),
        Coordinates(2, 124, 11)
      )
    )
  }

  def initialActionsV2(): List[OrdonAction] = {
    List(
      new KeepActiveOrdonAction(allPlanetsAndMoons),
      //      new ExpeditionMoveResourcesAndFleetOrdonAction(planet10, moon10, expeditionFleet),
      //      new DeployAndReturnOrdonAction(planet10, moon10),
      //      new TransportToOrdonAction(List(planet3, planet6, planet7, planet7_154, planet13), planet10),
      //      new ResearchOrdonAction(planet10, researchList),
      new ExpeditionCollectDebrisOrdonAction(moon10),
      new ExpeditionOrdonAction(moon10),
      //      monitorSos(),
      //      monitorRadamantys(),
      //      monitorVed(),
      monitorAdmiralSun(),
      monitorAdmiralMagnetar(),
      new StatusAction(expeditionFleet)
    )
  }

  private val researchList =
    List(Computer -> 14, Computer -> 15, Weapons -> 14, Shielding -> 14, Armor -> 19)

  private val expeditionFleet: Map[ShipType, Int] =
    Map(Destroyer -> 1, EspionageProbe -> 1, LargeCargoShip -> 1000, Explorer -> 250, LightFighter -> 1700)

  private val planet7_154 = PlayerPlanet(PlanetId.apply("33624816"), Coordinates(1, 154, 7))
  private val planet3 = PlayerPlanet(PlanetId.apply("33635734"), Coordinates(1, 155, 3))
  private val moon3 = PlayerPlanet(PlanetId.apply("33639213"), Coordinates(1, 155, 3, Moon))
  private val planet6 = PlayerPlanet(PlanetId.apply("33635176"), Coordinates(1, 155, 6))
  private val moon6 = PlayerPlanet(PlanetId.apply("33647235"), Coordinates(1, 155, 6, Moon))
  private val planet7 = PlayerPlanet(PlanetId.apply("33623552"), Coordinates(1, 155, 7))
  private val moon7 = PlayerPlanet(PlanetId.apply("33635018"), Coordinates(1, 155, 7, Moon))
  private val planet10 = PlayerPlanet(PlanetId.apply("33620959"), Coordinates(1, 155, 10))
  private val moon10 = PlayerPlanet(PlanetId.apply("33632870"), Coordinates(1, 155, 10, Moon))
  private val planet13 = PlayerPlanet(PlanetId.apply("33629451"), Coordinates(1, 155, 13))
  private val moon13 = PlayerPlanet(PlanetId.apply("33639213"), Coordinates(1, 155, 3, Moon))
  private val allPlanetsAndMoons =
    List(planet7_154, planet3, moon3, planet6, moon6, planet7, moon7, planet10, moon10, planet13, moon13)
}
