package not.ogame.bots.ghostbuster.processors

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.ghostbuster.FLogger
import not.ogame.bots.ghostbuster.executor.{OgameActionExecutor, _}
import not.ogame.bots.ghostbuster.ogame.{OgameAction, OgameActionDriver}

import scala.concurrent.duration.FiniteDuration

class SendShipsProcessor(config: SendShipConfig, driver: OgameActionDriver)(implicit executor: OgameActionExecutor[Task], clock: LocalClock)
    extends FLogger {
  def run(): Task[Unit] = {
    driver
      .readPlanets()
      .execute()
      .flatMap { planets =>
        loop(planets.find(_.id == config.from).get, planets.find(_.id == config.to).get)
      }
  }

  def loop(from: PlayerPlanet, to: PlayerPlanet): Task[Unit] = {
    for {
      _ <- checkAndSend(from, to)
      _ <- executor.waitTo(clock.now().plus(config.interval))
      _ <- loop(from, to)
    } yield ()
  }

  private def checkAndSend(from: PlayerPlanet, to: PlayerPlanet) = {
    (for {
      fleetPageData <- driver.readFleetPage(from.id)
      interestingShips = config.select(fleetPageData)
      _ <- if (interestingShips.nonEmpty) {
        Logger[OgameAction].info(s"Found some interesting ships $interestingShips on planet. Sending them to target...") >>
          driver.sendFleet(
            SendFleetRequest(
              from,
              SendFleetRequestShips.Ships(interestingShips),
              to.coordinates,
              FleetMissionType.Deployment,
              FleetResources.Given(Resources.Zero)
            )
          )
      } else {
        ().pure[OgameAction]
      }
    } yield ()).execute()
  }
}

case class SendShipConfig(from: PlanetId, to: PlanetId, selectors: List[ShipSelector], interval: FiniteDuration) {
  def select(fleetPageData: FleetPageData): Map[ShipType, Int] = {
    selectors.foldLeft(Map.empty[ShipType, Int])((acc, item) => acc + item.select(fleetPageData)).filter(_._2 > 0)
  }
}

case class ShipSelector(shipType: ShipType, decreaseBy: Int = 0) {
  def select(fleetPageData: FleetPageData): (ShipType, Int) = {
    shipType -> Math.max(fleetPageData.ships(shipType) - decreaseBy, 0)
  }
}
