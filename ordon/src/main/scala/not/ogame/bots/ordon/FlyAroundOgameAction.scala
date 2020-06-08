package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots.SendFleetRequestShips.Ships
import not.ogame.bots._
import not.ogame.bots.ordon.utils.{SelectResources, SelectShips}

import scala.util.Random

class FlyAroundOgameAction[T[_]: Monad](
    speed: FleetSpeed,
    targets: List[PlayerPlanet],
    fleetSelector: PlayerPlanet => SelectShips,
    resourceSelector: PlayerPlanet => SelectResources
)(implicit clock: LocalClock)
    extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] = {
    ogame.readAllFleets().flatMap(fleets => findThisFleet(fleets).map(getResumeOnForFleet).getOrElse(sendFleet(ogame)))
  }

  private def findThisFleet(fleets: List[Fleet]): Option[Fleet] = {
    fleets.find(isThisFleet)
  }

  private def isThisFleet(fleet: Fleet): Boolean = {
    targets.map(_.coordinates).contains(fleet.from) &&
    targets.map(_.coordinates).contains(fleet.to) &&
    fleet.fleetMissionType == Deployment
  }

  private def getResumeOnForFleet(fleet: Fleet): T[ZonedDateTime] = {
    // There is an issue in ogame that fleet just after arrival is still on fleet list as returning. To avoid that 3 second delay was added.
    fleet.arrivalTime.plusSeconds(3).pure[T]
  }

  private def sendFleet(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      planetToShips <- targets
        .map(planet => ogame.readFleetPage(planet.id).map(page => fleetSelector(planet)(page)).map(planet -> _))
        .sequence
      (startPlanet, ships) = planetToShips.maxBy(it => it._2.values.sum)
      targetPlanet = Random.shuffle(targets.filter(_ != startPlanet)).head
      resources <- ogame.readFleetPage(startPlanet.id).map(page => resourceSelector(startPlanet)(page))
      _ <- ogame.sendFleet(
        SendFleetRequest(
          startPlanet,
          ships = Ships(ships),
          targetCoordinates = targetPlanet.coordinates,
          fleetMissionType = Deployment,
          resources = FleetResources.Given(resources),
          speed = speed
        )
      )
      now = clock.now()
    } yield now
}
