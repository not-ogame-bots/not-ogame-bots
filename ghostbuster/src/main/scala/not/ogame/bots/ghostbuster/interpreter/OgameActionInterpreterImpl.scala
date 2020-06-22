package not.ogame.bots.ghostbuster.interpreter

import java.time.ZonedDateTime

import cats.effect.Async
import cats.implicits._
import cats.~>
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.reactive.Observable
import not.ogame.bots.ghostbuster.executor.OgameActionExecutor
import not.ogame.bots.ghostbuster.notifications.{Notification, NotificationAware}
import not.ogame.bots.ghostbuster.ogame.{OgameAction, OgameOp}
import not.ogame.bots.ghostbuster.{FLogger, processors}
import not.ogame.bots.{LocalClock, OgameDriver}

import scala.concurrent.duration._

class OgameActionInterpreterImpl(ogameDriver: OgameDriver[Task] with NotificationAware, taskExecutor: TaskExecutor[Task])(
    implicit clock: LocalClock
) extends OgameActionExecutor[Task]
    with FLogger {
  private val compiler: OgameOp ~> Task = new (OgameOp ~> Task) {
    override def apply[A](fa: OgameOp[A]): Task[A] = {
      Logger[Task].debug(s"Executing action: $fa") >>
        (fa match {
          case OgameOp.ReadSupplyPage(planetId) =>
            ogameDriver.readSuppliesPage(planetId)
          case OgameOp.ReadTechnologyPage(planetId) =>
            ogameDriver.readTechnologyPage(planetId)
          case OgameOp.ReadFacilityPage(planetId) =>
            ogameDriver.readFacilityPage(planetId)
          case OgameOp.ReadFleetPage(planetId) =>
            ogameDriver.readFleetPage(planetId)
          case OgameOp.ReadAllFleets() =>
            ogameDriver.readAllFleets()
          case OgameOp.ReadMyFleets() =>
            ogameDriver.readMyFleets()
          case OgameOp.ReadPlanets() =>
            ogameDriver.readPlanets()
          case OgameOp.CheckLoginStatus() =>
            ogameDriver.checkIsLoggedIn()
          case OgameOp.ReadMyOffers() =>
            ogameDriver.readMyOffers()
          case OgameOp.Login() =>
            ogameDriver.login()
          case OgameOp.BuildSuppliesBuilding(planetId, suppliesBuilding) =>
            ogameDriver.buildSuppliesBuilding(planetId, suppliesBuilding)
          case OgameOp.BuildFacilityBuilding(planetId, facilityBuilding) =>
            ogameDriver.buildFacilityBuilding(planetId, facilityBuilding)
          case OgameOp.StartResearch(planetId, technology) =>
            ogameDriver.startResearch(planetId, technology)
          case OgameOp.BuildShip(planetId, shipType, amount) =>
            ogameDriver.buildShips(planetId, shipType, amount)
          case OgameOp.SendFleet(request) =>
            ogameDriver.sendFleet(request)
          case OgameOp.ReturnFleet(fleetId) =>
            ogameDriver.returnFleet(fleetId)
          case OgameOp.CreateOffer(planetId, newOffer) =>
            ogameDriver.createOffer(planetId, newOffer)
          case OgameOp.RaiseError(throwable) =>
            Task.raiseError(throwable)
          case OgameOp.HandleError(fa, f) =>
            val fa2 = fa.foldMap(compiler)
            val f2 = f.andThen(_.foldMap(compiler))
            fa2.handleErrorWith(f2)
          case OgameOp.BracketCase(acquire, use, release) =>
            implicitly[Async[Task]].bracketCase(acquire.foldMap(compiler))(use(_).foldMap(compiler))(release(_, _).foldMap(compiler))
          case OgameOp.BuildSolarSatellite(planetId, count) =>
            ogameDriver.buildSolarSatellites(planetId, count)
        })
    }
  }

  override def execute[A](action: OgameAction[A]): Task[A] = taskExecutor.exec(action.foldMap(compiler))

  override def waitTo(time: ZonedDateTime): Task[Unit] = {
    Task.eval(clock.now()).flatMap { now =>
      val sleepTime = processors.timeDiff(now, processors.max(now, time))
      Logger[Task].debug(s"sleeping ~ ${sleepTime.toSeconds / 60} minutes til $time") >>
        Task.sleep(sleepTime.plus(2 seconds)) // additional 2 seconds to make things go smooth
    }
  }

  override def subscribeToNotifications: Observable[Notification] = ogameDriver.subscribeToNotifications
}
