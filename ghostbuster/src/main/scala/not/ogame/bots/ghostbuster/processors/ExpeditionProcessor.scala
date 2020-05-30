package not.ogame.bots.ghostbuster.processors

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.ghostbuster.{BotConfig, FLogger, PlanetFleet}

import scala.concurrent.duration._

//TODO optimize check fleet on planet - by lazy
class ExpeditionProcessor(botConfig: BotConfig, taskExecutor: TaskExecutor) extends FLogger {
  def run(): Task[Unit] = {
    if (botConfig.expeditionConfig.isOn) {
      taskExecutor
        .readPlanets()
        .flatMap(lookForFleet)
        .onErrorRestartIf(_ => true)
    } else {
      Task.never
    }
  }

  private def lookForFleet(planets: List[PlayerPlanet]): Task[Unit] = {
    for {
      fleets <- taskExecutor.readAllFleets()
      expeditions = fleets.filter(fleet => isExpedition(planets, fleet))
      returningExpeditions = expeditions.filter(_.isReturning)
      _ <- if (returningExpeditions.size < botConfig.expeditionConfig.maxNumberOfExpeditions) {
        Logger[Task].info(
          s"Only ${returningExpeditions.size}/${botConfig.expeditionConfig.maxNumberOfExpeditions} expeditions are in the air"
        ) >>
          lookForFleetOnPlanets(planets) >> lookForFleet(planets)
      } else {
        val min = expeditions.map(_.arrivalTime).min
        Logger[Task].info(s"All expeditions are in the air, waiting for first to reach its target - $min") >>
          taskExecutor.waitTo(min) >> lookForFleet(planets)
      }
    } yield ()
  }

  private def isExpedition(planets: List[PlayerPlanet], fleet: Fleet) = {
    fleet.fleetAttitude == FleetAttitude.Friendly && fleet.fleetMissionType == FleetMissionType.Expedition &&
    planets.exists(p => p.coordinates == fleet.from)
  }

  private def sendExpedition(fromPlanet: PlayerPlanet): Task[Unit] = {
    taskExecutor
      .readSupplyPage(fromPlanet)
      .flatMap { suppliesPageData =>
        if (suppliesPageData.currentResources.deuterium >= botConfig.expeditionConfig.deuterThreshold) {
          sendFleetImpl(fromPlanet)
        } else {
          val missingDeuter = botConfig.fsConfig.deuterThreshold - suppliesPageData.currentResources.deuterium
          val timeToProduceInHours = missingDeuter.toDouble / suppliesPageData.currentProduction.deuterium
          val timeInSeconds = (timeToProduceInHours * 60 * 60).toInt
          Logger[Task].info(s"There was not enough deuter, expedition sending delayed by $timeInSeconds seconds") >> Task.sleep(
            timeInSeconds seconds
          ) >> sendFleetImpl(fromPlanet)
        }
      }
    sendFleetImpl(fromPlanet)
  }

  private def sendFleetImpl(fromPlanet: PlayerPlanet) = {
    Logger[Task].info("Sending fleet...") >>
      taskExecutor
        .sendFleet(
          SendFleetRequest(
            fromPlanet,
            SendFleetRequestShips.Ships(botConfig.expeditionConfig.ships.map(s => s.shipType -> s.amount).toMap),
            fromPlanet.coordinates.copy(position = 16),
            FleetMissionType.Expedition,
            FleetResources.Given(Resources.Zero)
          )
        )
        .void
  }
  private def lookForFleetOnPlanets(planets: List[PlayerPlanet]) = {
    planets
      .map(taskExecutor.getFleetOnPlanet)
      .sequence
      .flatMap { planetFleets =>
        planetFleets
          .collectFirst { case PlanetFleet(planet, fleet) if isExpeditionFleet(fleet) => planet }
          .map(sendExpedition)
          .getOrElse {
            Logger[Task].info("Could find expedition fleet on any planet. Waiting 10 minutes....") >> Task.sleep(10 minutes)
          }
      }
  }
  private def isExpeditionFleet(fleet: Map[ShipType, Int]): Boolean = {
    botConfig.expeditionConfig.ships.forall(ship => ship.amount <= fleet(ship.shipType))
  }
}
