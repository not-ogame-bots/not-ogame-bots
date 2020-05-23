package not.ogame.bots.ordon

import java.time.Instant

import cats.effect.IO
import not.ogame.bots.ShipType.{Destroyer, EspionageProbe, Explorer, LargeCargoShip}
import not.ogame.bots.{Coordinates, Credentials}

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
    val listOfActions = List(createExpeditionAction)
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
}
