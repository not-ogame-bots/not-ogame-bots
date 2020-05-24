package not.ogame.bots.ordon

import java.time.Instant

import cats.effect.IO
import cats.implicits._
import not.ogame.bots.ShipType.{Destroyer, EspionageProbe, Explorer, LargeCargoShip}
import not.ogame.bots.{Coordinates, CoordinatesType, Credentials, PlayerPlanet}

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

  def getInitialActions: IO[List[ScheduledAction[IO]]] = {
    val listOfActions = List(createExpeditionAction, createFlyAroundActionCargo, createFlyAroundActionBattle)
    IO.pure(listOfActions.map(ScheduledAction(Instant.now(), _)))
  }

  private def createExpeditionAction: ExpeditionOgameAction[IO] = {
    new ExpeditionOgameAction[IO](
      maxNumberOfExpeditions = 5,
      startPlanetId = "33645302",
      expeditionFleet = Map(Destroyer -> 1, LargeCargoShip -> 300, Explorer -> 160, EspionageProbe -> 1),
      targetCoordinates = Coordinates(3, 133, 16)
    )
  }

  private def createFlyAroundActionCargo: FlyAroundOgameAction[IO] = {
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

  private def createFlyAroundActionBattle: FlyAroundOgameAction[IO] = {
    new FlyAroundOgameAction[IO](
      targets = List(moon4, moon5),
      fleetSelector = new FleetSelector[IO](),
      resourceSelector = new ResourceSelector[IO]()
    )
  }

  private val planet4 = PlayerPlanet("33638312", Coordinates(3, 133, 4))
  private val planet5 = PlayerPlanet("33642425", Coordinates(3, 133, 5))
  private val planet6 = PlayerPlanet("33641548", Coordinates(3, 133, 6))
  private val planet7 = PlayerPlanet("33639665", Coordinates(3, 133, 7))
  private val moon4 = PlayerPlanet("33645637", Coordinates(3, 133, 4, CoordinatesType.Moon))
  private val moon5 = PlayerPlanet("33645302", Coordinates(3, 133, 5, CoordinatesType.Moon))
  private val moon6 = PlayerPlanet("33645566", Coordinates(3, 133, 6, CoordinatesType.Moon))
  private val moon7 = PlayerPlanet("33657126", Coordinates(3, 133, 7, CoordinatesType.Moon))
}
