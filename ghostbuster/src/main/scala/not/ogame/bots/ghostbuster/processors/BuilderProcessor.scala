package not.ogame.bots.ghostbuster.processors

import java.time.ZonedDateTime

import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import not.ogame.bots.{LocalClock, OgameDriver, PlayerPlanet}
import not.ogame.bots.ghostbuster.{FLogger, SmartBuilderConfig}
import cats.implicits._
import not.ogame.bots.ghostbuster.executor._
import not.ogame.bots.ghostbuster.ogame.OgameAction

class BuilderProcessor(builder: Builder, config: SmartBuilderConfig, ogameDriver: OgameDriver[OgameAction])(
    implicit executor: OgameActionExecutor[Task],
    clock: LocalClock
) extends FLogger {
  def run(): Task[Unit] = {
    if (config.isOn) {
      ogameDriver
        .readPlanets()
        .execute()
        .flatMap(planets => Task.parSequence(planets.map(loopBuilder)))
        .void
    } else {
      Task.never
    }
  }

  private def loopBuilder(planet: PlayerPlanet): Task[Unit] = {
    builder
      .buildNextThingFromWishList(planet)
      .flatMap {
        case BuilderResult.Building(finishTime) =>
          Logger[OgameAction].info(s"Waiting for building to finish $finishTime").as(finishTime)
        case BuilderResult.Waiting(waitingTime) =>
          firstFleetArrivalTime(planet).flatMap {
            case Some(arrivalTime) if arrivalTime.isBefore(waitingTime) =>
              Logger[OgameAction].info(s"Waiting for first fleet to arrive til $arrivalTime").as(arrivalTime)
            case _ =>
              val limitedWaitingTime = max(waitingTime, clock.now().plus(config.interval))
              Logger[OgameAction].info(s"Waiting for resources to produce til $limitedWaitingTime").as(limitedWaitingTime)
          }
        case BuilderResult.Idle => clock.now().plus(config.interval).pure[OgameAction]
      }
      .execute()
      .flatMap(waitTime => executor.waitTo(waitTime) >> loopBuilder(planet))
  }

  private def firstFleetArrivalTime(planet: PlayerPlanet) = {
    ogameDriver
      .readAllFleetsRedirect()
      .map(_.filter(f => f.to == planet.coordinates))
      .map {
        case l if l.nonEmpty => l.map(_.arrivalTime).min.some
        case Nil             => Option.empty[ZonedDateTime]
      }
  }
}
