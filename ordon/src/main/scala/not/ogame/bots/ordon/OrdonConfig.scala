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
    val listOfActions = List(createExpeditionAction, createKeepActiveAction, createFlyAroundActionCargo, createFlyAroundActionBattle)
    IO.pure(listOfActions.map(ScheduledAction(clock.now(), _)))
  }

  private def createExpeditionAction(implicit clock: LocalClock): ExpeditionOgameAction[IO] = {
    new ExpeditionOgameAction[IO](
      maxNumberOfExpeditions = 6,
      startPlanet = moon5,
      expeditionFleet = Map(Destroyer -> 1, LargeCargoShip -> 300, Explorer -> 300, EspionageProbe -> 1),
      targetCoordinates = Coordinates(3, 133, 16)
    )
  }

  private def createKeepActiveAction(implicit clock: LocalClock): KeepActiveOgameAction[IO] = {
    new KeepActiveOgameAction[IO](planetsAndMoons)
  }

  private def createFlyAroundActionCargo(implicit clock: LocalClock): FlyAroundOgameAction[IO] = {
    new FlyAroundOgameAction[IO](
      targets = List(moon4, moon5),
      fleetSelector = new FleetSelector[IO](
        filters = Map(
          Destroyer -> Selector.skip,
          EspionageProbe -> Selector.skip,
          Explorer -> Selector.skip,
          LargeCargoShip -> Selector.decreaseBy(300)
        )
      ),
      resourceSelector = new ResourceSelector[IO](deuteriumSelector = Selector.decreaseBy(200_000))
    )
  }

  private def createFlyAroundActionBattle(implicit clock: LocalClock): FlyAroundOgameAction[IO] = {
    new FlyAroundOgameAction[IO](
      targets = List(moon6, moon7),
      fleetSelector = new FleetSelector[IO](),
      resourceSelector = new ResourceSelector[IO]()
    )
  }

  private val planet4 = PlayerPlanet("33638312", Coordinates(3, 133, 4))
  private val planet5 = PlayerPlanet("33642425", Coordinates(3, 133, 5))
  private val planet6 = PlayerPlanet("33641548", Coordinates(3, 133, 6))
  private val planet7 = PlayerPlanet("33639665", Coordinates(3, 133, 7))
  private val planet8 = PlayerPlanet("33657297", Coordinates(3, 133, 8))
  private val moon4 = PlayerPlanet("33645637", Coordinates(3, 133, 4, CoordinatesType.Moon))
  private val moon5 = PlayerPlanet("33645302", Coordinates(3, 133, 5, CoordinatesType.Moon))
  private val moon6 = PlayerPlanet("33645566", Coordinates(3, 133, 6, CoordinatesType.Moon))
  private val moon7 = PlayerPlanet("33657126", Coordinates(3, 133, 7, CoordinatesType.Moon))
  private val moon8 = PlayerPlanet("33658754", Coordinates(3, 133, 8, CoordinatesType.Moon))
  private val planets: List[PlayerPlanet] = List(planet4, planet5, planet6, planet7, planet8)
  private val moons: List[PlayerPlanet] = List(moon4, moon5, moon6, moon7, moon8)
  private val planetsAndMoons: List[PlayerPlanet] = planets ++ moons
}
