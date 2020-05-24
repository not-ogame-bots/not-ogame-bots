package not.ogame.bots.ghostbuster.processors

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import monix.eval.Task
import not.ogame.bots.ghostbuster.PlanetFleet
import not.ogame.bots._

import scala.concurrent.duration.FiniteDuration

trait TaskExecutor {
  def waitSeconds(duration: FiniteDuration): Task[Unit]

  def readAllFleets(): Task[List[Fleet]]

  def readPlanets(): Task[List[PlayerPlanet]]

  def sendFleet(req: SendFleetRequest): Task[Instant]

  def getFleetOnPlanet(planet: PlayerPlanet): Task[PlanetFleet]

  def readSupplyPage(playerPlanet: PlayerPlanet): Task[SuppliesPageData]

  def buildSupplyBuilding(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, planet: PlayerPlanet): Task[Instant]
}
