package not.ogame.bots.ordon

import cats.effect.IO
import cats.implicits._
import not.ogame.bots._

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
      new StartBuildingsOgameAction[IO]()
    )
    IO.pure(listOfActions.map(ScheduledAction(clock.now(), _)))
  }
}
