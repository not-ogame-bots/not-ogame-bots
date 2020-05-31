package not.ogame.bots.ghostbuster.processors

import cats.implicits._
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.ghostbuster.BotConfig
import SimplifiedDataTime._

class EscapeFleetProcessor(taskExecutor: TaskExecutor, botConfig: BotConfig)(implicit clock: LocalClock) {
  def run(): Task[Unit] = {
    taskExecutor
      .readPlanets()
      .flatMap(planets => Task.parSequence(planets.map(check)))
      .void
  }

  private def check(planet: PlayerPlanet): Task[Unit] = {
    taskExecutor
      .readAllFleets()
      .flatMap { fleets =>
        val hostileFleets = fleets
          .filter(f => isHostileToGivenPlanet(planet, f))
          .sortBy(_.arrivalTime) //hope it does ascending
        if (hostileFleets.nonEmpty) {
          hostileFleets.traverse(hf => escapeSingleFleet(planet, hf)) >>
            waitAndCheck(planet)
        } else {
          waitAndCheck(planet)
        }
      }
  }

  private def isHostileToGivenPlanet(planet: PlayerPlanet, f: Fleet) = {
    f.fleetAttitude == FleetAttitude.Hostile && f.arrivalTime
      .isAfter(clock.now().plusSeconds(botConfig.escapeConfig.interval.toSeconds)) && //TODO check time is bigger than safe time
    f.to == planet.coordinates
  }

  private def escapeSingleFleet(planet: PlayerPlanet, hf: Fleet) = {
    taskExecutor.waitTo(hf.arrivalTime.minusSeconds(20)) >> taskExecutor.sendFleet( //TODO check there is any fleet on planet
      SendFleetRequest(
        planet,
        SendFleetRequestShips.AllShips,
        botConfig.escapeConfig.target,
        FleetMissionType.Transport,
        FleetResources.Max
      )
    ) >> taskExecutor.waitTo(hf.arrivalTime.plusSeconds(5)) >> taskExecutor.cancelFleet(planet.coordinates) //TODO safe time plus
    //TODO check if attack ended
  }
  private def waitAndCheck(planet: PlayerPlanet) = {
    taskExecutor.waitTo(clock.now().plusSeconds(botConfig.escapeConfig.interval.toSeconds)) >> check(planet)
  }
}
