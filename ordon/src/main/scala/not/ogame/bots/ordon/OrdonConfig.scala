package not.ogame.bots.ordon

import cats.effect.IO
import cats.implicits._
import not.ogame.bots.OfferItemType.{Crystal, Deuterium, Metal}
import not.ogame.bots.ShipType.{Destroyer, EspionageProbe, Explorer, LargeCargoShip}
import not.ogame.bots._
import not.ogame.bots.ordon.utils.{FleetSelector, ResourceSelector, Selector}

object OrdonConfig {
  def getCredentials: Credentials = {
    val source = scala.io.Source.fromFile(s"${System.getenv("HOME")}/.not-ogame-bots/credentials.conf")
    val credentials = source
      .getLines()
      .toList
      .map(_.split(":")(1).trim.drop(1).dropRight(1))
    source.close()
    Credentials(credentials.head, credentials(1), credentials(2), credentials(3))
  }

  class OgameConfig extends CargoProcessConfig with BattleProcessConfig {
    override val expeditionMoon: PlayerPlanet = moon5
    override val otherMoon: PlayerPlanet = moon4
    override val fsPlanet: PlayerPlanet = planet6
    override val fsMoon: PlayerPlanet = moon6
    override val safeBufferInMinutes: Int = 5 //50
    override val randomUpperLimitInSeconds: Int = 120 // 1
    override val expectedOffers: List[MyOffer] = List(
      MyOffer(Metal, 1_000_000, Deuterium, 280_000),
      MyOffer(Metal, 1_000_000, Deuterium, 280_000),
      MyOffer(Metal, 1_000_000, Deuterium, 280_000),
      MyOffer(Metal, 1_000_000, Deuterium, 280_000),
      MyOffer(Metal, 1_000_000, Deuterium, 280_000),
      MyOffer(Metal, 1_000_000, Crystal, 420_000),
      MyOffer(Metal, 1_000_000, Crystal, 420_000),
      MyOffer(Metal, 5_000_000, Deuterium, 1_400_000),
      MyOffer(Metal, 5_000_000, Deuterium, 1_400_000),
      MyOffer(Metal, 5_000_000, Deuterium, 1_400_000)
    )
  }

  def getInitialActions(implicit clock: LocalClock): IO[List[ScheduledAction[IO]]] = {
    val configClass = new OgameConfig()
    val listOfActions = List(
      new FsCargoProcess[IO](configClass).startAction(),
      new FsBattleProcess[IO](configClass).startAction(),
      //      createFlyAroundActionCargo(moon4),
      //      new DeployAndReturnOgameAction[IO](planet6, moon6),
      createExpeditionAction,
      createKeepActiveAction,
      new AlertOgameAction[IO]()
    )
    IO.pure(listOfActions.map(ScheduledAction(clock.now(), _)))
  }

  private def createExpeditionAction(implicit clock: LocalClock): ExpeditionOgameAction[IO] = {
    new ExpeditionOgameAction[IO](
      maxNumberOfExpeditions = 6,
      startPlanet = expeditionStartPlanet,
      expeditionFleet = Map(Destroyer -> 1, LargeCargoShip -> 410, Explorer -> 670, EspionageProbe -> 1),
      targetCoordinates = Coordinates(3, 133, 16)
    )
  }

  private def createKeepActiveAction(implicit clock: LocalClock): KeepActiveOgameAction[IO] = {
    new KeepActiveOgameAction[IO](planetsAndMoons)
  }

  private def createFlyAroundActionCargo(moon: PlayerPlanet)(implicit clock: LocalClock) = {
    new FlyAroundOgameAction[IO](
      speed = FleetSpeed.Percent30,
      targets = List(moon, expeditionStartPlanet),
      fleetSelector = cargoFleetSelector,
      resourceSelector = cargoResourceSelector
    )
  }

  private def cargoFleetSelector: PlayerPlanet => FleetSelector = { playerPlanet =>
    if (playerPlanet == expeditionStartPlanet) {
      new FleetSelector(
        filters = Map(
          Destroyer -> Selector.decreaseBy(6),
          EspionageProbe -> Selector.decreaseBy(50),
          Explorer -> Selector.skip,
          LargeCargoShip -> Selector.decreaseBy(410)
        )
      )
    } else {
      new FleetSelector()
    }
  }

  private def cargoResourceSelector: PlayerPlanet => ResourceSelector = { playerPlanet =>
    if (playerPlanet == expeditionStartPlanet) {
      new ResourceSelector(deuteriumSelector = Selector.decreaseBy(300_000))
    } else {
      new ResourceSelector()
    }
  }

  private def createFlyAroundActionBattle(implicit clock: LocalClock): FlyAroundOgameAction[IO] = {
    new FlyAroundOgameAction[IO](
      speed = FleetSpeed.Percent10,
      targets = List(moon6, moon7),
      fleetSelector = _ => new FleetSelector(),
      resourceSelector = _ => new ResourceSelector()
    )
  }

  private val planet4 = PlayerPlanet(PlanetId("33638312"), Coordinates(3, 133, 4))
  private val planet5 = PlayerPlanet(PlanetId("33642425"), Coordinates(3, 133, 5))
  private val planet6 = PlayerPlanet(PlanetId("33641548"), Coordinates(3, 133, 6))
  private val planet7 = PlayerPlanet(PlanetId("33639665"), Coordinates(3, 133, 7))
  private val planet8 = PlayerPlanet(PlanetId("33657297"), Coordinates(3, 133, 8))
  private val moon4 = PlayerPlanet(PlanetId("33645637"), Coordinates(3, 133, 4, CoordinatesType.Moon))
  private val moon5 = PlayerPlanet(PlanetId("33645302"), Coordinates(3, 133, 5, CoordinatesType.Moon))
  private val moon6 = PlayerPlanet(PlanetId("33645566"), Coordinates(3, 133, 6, CoordinatesType.Moon))
  private val moon7 = PlayerPlanet(PlanetId("33657126"), Coordinates(3, 133, 7, CoordinatesType.Moon))
  private val moon8 = PlayerPlanet(PlanetId("33658754"), Coordinates(3, 133, 8, CoordinatesType.Moon))
  private val planets: List[PlayerPlanet] = List(planet4, planet5, planet6, planet7, planet8)
  private val moons: List[PlayerPlanet] = List(moon4, moon5, moon6, moon7, moon8)
  private val planetsAndMoons: List[PlayerPlanet] = planets ++ moons
  private val expeditionStartPlanet = moon5
}
