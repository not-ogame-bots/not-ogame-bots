package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import monix.eval.Task
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots._
import not.ogame.bots.ghostbuster.FlyAndReturnConfig

import scala.util.Random
import scala.concurrent.duration._

class FlyAndReturnProcessor(config: FlyAndReturnConfig, taskExecutor: TaskExecutor)(implicit clock: LocalClock) {
  def run(): Task[Unit] = {
    if (config.isOn) {
      for {
        planets <- taskExecutor.readPlanetsAndMoons()
        from = planets.find(p => p.id == config.from).get
        to = planets.find(p => p.id == config.to).get
        _ <- loop(from, to)
      } yield ()
    } else {
      Task.never
    }
  }

  private def loop(from: PlayerPlanet, to: PlayerPlanet): Task[Unit] = {
    for {
      allMyFleets <- taskExecutor.readMyFleets()
      thisMyFleet = allMyFleets.find(isThisMyFleet(_, from, to))
      expeditionsCount = allMyFleets.count(_.fleetMissionType == FleetMissionType.Expedition)
      nextStepTime <- processMyFleet(thisMyFleet, from, to, expeditionsCount)
      _ <- taskExecutor.waitTo(nextStepTime)
      _ <- loop(from, to)
    } yield ()
  }

  private def processMyFleet(thisMyFleet: Option[MyFleet], from: PlayerPlanet, to: PlayerPlanet, expeditions: Int): Task[ZonedDateTime] = {
    if (expeditions < 5) { //TODO configurable
      Task.pure(clock.now().plus(5 seconds))
    } else {
      thisMyFleet match {
        case Some(fleet) if !fleet.isReturning => returnOrWait(fleet)
        case Some(fleet) if fleet.isReturning  => Task.pure(fleet.arrivalTime.plus(3 seconds))
        case None =>
          new ResourceSelector[Task](deuteriumSelector = Selector.decreaseBy(config.remainDeuterAmount))
            .selectResources(taskExecutor, from)
            .flatMap { resources =>
              send(from, to, resources) >> Task.pure(clock.now())
            }
      }
    }
  }

  private def returnOrWait(fleet: MyFleet): Task[ZonedDateTime] = {
    if (isCloseToArrival(fleet)) {
      taskExecutor.returnFleet(fleet.fleetId)
    } else {
      Task.pure(chooseTimeWhenClickReturn(fleet))
    }
  }

  private def send(from: PlayerPlanet, to: PlayerPlanet, resources: Resources): Task[ZonedDateTime] = {
    val fleetSpeed = Random.shuffle(List(FleetSpeed.Percent10, FleetSpeed.Percent20)).head
    taskExecutor
      .sendFleet(
        SendFleetRequest(
          from = from,
          ships = SendFleetRequestShips.AllShips,
          targetCoordinates = to.coordinates,
          fleetMissionType = Deployment,
          resources = FleetResources.Given(resources),
          speed = fleetSpeed
        )
      )
  }

  private def isCloseToArrival(fleet: MyFleet) = {
    fleet.arrivalTime.minus(config.safeBuffer).minus(config.randomUpperLimit).isBefore(clock.now())
  }

  private def chooseTimeWhenClickReturn(fleet: MyFleet): ZonedDateTime = {
    fleet.arrivalTime.minus(config.safeBuffer).minusSeconds(Random.nextLong(config.randomUpperLimit.toSeconds))
  }

  private def isThisMyFleet(fleet: MyFleet, from: PlayerPlanet, to: PlayerPlanet): Boolean = {
    fleet.from == from.coordinates && fleet.to == to.coordinates && fleet.fleetMissionType == FleetMissionType.Deployment
  }
}