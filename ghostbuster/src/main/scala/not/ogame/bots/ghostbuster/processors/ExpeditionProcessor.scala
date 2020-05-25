package not.ogame.bots.ghostbuster.processors

import cats.implicits._
import monix.eval.Task
import not.ogame.bots.ghostbuster.{BotConfig, PlanetFleet}
import not.ogame.bots._

class ExpeditionProcessor(botConfig: BotConfig, taskExecutor: TaskExecutor) {
  def run(): Task[Unit] = {
    if (botConfig.expeditionConfig.isOn) {
      taskExecutor.readPlanets().flatMap(lookForFleet)
    } else {
      Task.never
    }
  }

  private def lookForFleet(planets: List[PlayerPlanet]): Task[Unit] = {
    for {
      fleets <- taskExecutor.readAllFleets()
      matchedFleets = fleets.filter( //TODO check size
        f =>
          f.fleetAttitude == FleetAttitude.Friendly && f.fleetMissionType == FleetMissionType.Expedition && planets
            .exists(p => p.coordinates == f.from)
      )
      _ <- matchedFleets.find(!_.isReturning).orElse(matchedFleets.find(_.isReturning)) match {
        case Some(fleet) if fleet.isReturning =>
          println(s"Found our returning expedition fleet in the air: ${pprint.apply(fleet)}")
          val fromPlanet = planets.find(p => fleet.from == p.coordinates).get
          taskExecutor.waitTo(fleet.arrivalTime) >> sendExpedition(fromPlanet)
        case Some(fleet) if !fleet.isReturning =>
          println(s"Found our outgoing expedition fleet in the air: ${pprint.apply(fleet)}")
          taskExecutor.waitTo(fleet.arrivalTime) >> lookForFleet(planets)
        case None => lookForFleetOnPlanets(planets)
      }
    } yield ()
  }

  private def sendExpedition(fromPlanet: PlayerPlanet): Task[Unit] = {
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
      .flatMap(arrivalTime => taskExecutor.waitTo(arrivalTime))
      .flatMap(_ => sendExpedition(fromPlanet)) //TODO can the fleet be delayed by expedition?
  }

  private def lookForFleetOnPlanets(planets: List[PlayerPlanet]) = {
    planets
      .map(p => taskExecutor.getFleetOnPlanet(p))
      .sequence
      .flatMap { planetFleets =>
        planetFleets.find(isExpeditionFleet) match {
          case Some(planet) =>
            println(s"Planet with expedition fleet ${pprint.apply(planet)}")
            sendExpedition(planet.playerPlanet)
          case None =>
            println("Couldn't find expedition fleet on any planet")
            Task.never
        }
      }
  }
  private def isExpeditionFleet(planetFleet: PlanetFleet): Boolean = {
    botConfig.expeditionConfig.ships.forall(ship => ship.amount <= planetFleet.fleet(ship.shipType))
  }
}
