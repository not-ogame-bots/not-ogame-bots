package not.ogame.bots.ordon

import cats.effect.IO
import cats.implicits._
import not.ogame.bots.ShipType.{Destroyer, EspionageProbe, Explorer, LargeCargoShip}
import not.ogame.bots._

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

  def getInitialActions(implicit clock: LocalClock): IO[List[ScheduledAction[IO]]] = {
    val listOfActions = List(
      //      new BuildShipsOgameAction[IO](ShipType.Cruiser, 2000, planet6, planets),
      createFlyAroundActionCargo(moon4),
      //      new DeployAndReturnOgameAction[IO](planet4, moon4),
      //      new DeployAndReturnOgameAction[IO](planet5, moon5),
      new DeployAndReturnOgameAction[IO](planet6, moon6),
      //      new DeployAndReturnOgameAction[IO](planet6, moon6, safeBufferInMinutes = 50, randomUpperLimitInSeconds = 1),
      //      new DeployAndReturnOgameAction[IO](planet7, moon7),
      //      new DeployAndReturnOgameAction[IO](planet8, moon8),
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

  private def cargoFleetSelector: PlayerPlanet => FleetSelector[IO] = { playerPlanet =>
    if (playerPlanet == expeditionStartPlanet) {
      new FleetSelector[IO](
        filters = Map(
          Destroyer -> Selector.decreaseBy(6),
          EspionageProbe -> Selector.decreaseBy(50),
          Explorer -> Selector.skip,
          LargeCargoShip -> Selector.decreaseBy(410)
        )
      )
    } else {
      new FleetSelector[IO]()
    }
  }

  private def cargoResourceSelector: PlayerPlanet => ResourceSelector[IO] = { playerPlanet =>
    if (playerPlanet == expeditionStartPlanet) {
      new ResourceSelector[IO](deuteriumSelector = Selector.decreaseBy(300_000))
    } else {
      new ResourceSelector[IO]()
    }
  }

  private def createFlyAroundActionBattle(implicit clock: LocalClock): FlyAroundOgameAction[IO] = {
    new FlyAroundOgameAction[IO](
      speed = FleetSpeed.Percent10,
      targets = List(moon6, moon7),
      fleetSelector = _ => new FleetSelector[IO](),
      resourceSelector = _ => new ResourceSelector[IO]()
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
