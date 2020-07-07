package not.ogame.bots.ghostbuster.processors

import java.util.concurrent.ConcurrentHashMap

import cats.data.OptionT
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.{Fiber, Task}
import monix.reactive.Consumer
import not.ogame.bots.ghostbuster.FLogger
import not.ogame.bots.ghostbuster.executor.{OgameActionExecutor, _}
import not.ogame.bots.ghostbuster.notifications.{Notification, Notifier}
import not.ogame.bots.ghostbuster.ogame.OgameActionDriver
import not.ogame.bots.{FleetId, FleetMissionType, MyFleet}

class FleetTracker(notifier: Notifier, ogameActionDriver: OgameActionDriver)(implicit executor: OgameActionExecutor[Task]) extends FLogger {
  private val fleets = new ConcurrentHashMap[FleetId, Fiber[Unit]]()

  def run(): Task[Unit] = {
    ogameActionDriver
      .readMyFleets()
      .execute()
      .flatMap { myFleetPageData =>
        myFleetPageData.fleets
          .map(trackFleet)
          .sequence
      }
      .flatMap { _ =>
        withRetry(Task.parSequence(List(listenAndTrack, listenAndCancel)))("fleetTracker")
      }
      .void
  }

  private def listenAndCancel = {
    notifier.subscribeToNotifications
      .collect {
        case Notification.ReturnFleet(fleetId) => fleetId
      }
      .consumeWith(Consumer.foreachTask { fleetId =>
        ogameActionDriver
          .readMyFleets()
          .execute()
          .flatTap(_ => fleets.get(fleetId).cancel)
          .map(myFleetsPage => myFleetsPage.fleets.find(_.fleetId == fleetId).get)
          .flatMap { fleet =>
            executor
              .waitTo(fleet.arrivalTime)
              .flatMap(_ => notifier.notify(Notification.FleetArrived(fleet.from)))
          }
      })
  }

  private def listenAndTrack = {
    notifier.subscribeToNotifications
      .collect {
        case Notification.SendFleet(sendFleetRequest) => sendFleetRequest
      }
      .consumeWith(Consumer.foreachTask { sendFleetRequest =>
        ogameActionDriver
          .readMyFleets()
          .execute()
          .map { myFleetsPage =>
            myFleetsPage.fleets
              .filter(
                f => f.from == sendFleetRequest.from.coordinates && f.to == sendFleetRequest.targetCoordinates && !f.isReturning && f.isReturnable
              )
              .maxBy(_.arrivalTime)
          }
          .flatMap(trackFleet)
      })
  }

  private def trackFleet(fleet: MyFleet) = {
    Logger[Task].info(s"New fleet detected, will notify at ${fleet.arrivalTime}") >>
      (fleet.fleetMissionType match {
        case FleetMissionType.Deployment =>
          (executor.waitTo(fleet.arrivalTime) >>
            notifier.notify(Notification.FleetArrived(fleet.to))).start
            .map(f => fleets.put(fleet.fleetId, f))
            .void
        case FleetMissionType.Expedition =>
          handleExpedition(fleet).startAndForget
        case _ => Task.unit
      })
  }

  private def handleExpedition(fleet: MyFleet) = {
    if (fleet.isReturning) {
      (for {
        _ <- OptionT.liftF(executor.waitTo(fleet.arrivalTime))
        _ <- OptionT.liftF(notifier.notify(Notification.ExpeditionReturned(fleet.from, fleet.fleetId)))
      } yield ()).getOrElseF(notifier.notify(Notification.ExpeditionDestroyed(fleet.from, fleet.fleetId)))
    } else if (fleet.isReturnable) {
      (for {
        _ <- OptionT.liftF(executor.waitTo(fleet.arrivalTime))
        expFleetPhaseOne <- OptionT(getFleetData(fleet))
        _ <- OptionT.liftF(notifier.notify(Notification.ExpeditionPhaseOneCompleted(fleet.from, fleet.fleetId)))
        _ <- OptionT.liftF(executor.waitTo(expFleetPhaseOne.arrivalTime))
        expFleetPhaseTwo <- OptionT(getFleetData(fleet))
        _ <- OptionT.liftF(notifier.notify(Notification.ExpeditionPhaseTwoCompleted(fleet.from, fleet.fleetId)))
        _ <- OptionT.liftF(executor.waitTo(expFleetPhaseTwo.arrivalTime))
        _ <- OptionT.liftF(notifier.notify(Notification.ExpeditionReturned(fleet.from, fleet.fleetId)))
      } yield ()).getOrElseF(notifier.notify(Notification.ExpeditionDestroyed(fleet.from, fleet.fleetId)))
    } else {
      (for {
        _ <- OptionT.liftF(executor.waitTo(fleet.arrivalTime))
        expFleetPhaseTwo <- OptionT(getFleetData(fleet))
        _ <- OptionT.liftF(notifier.notify(Notification.ExpeditionPhaseTwoCompleted(fleet.from, fleet.fleetId)))
        _ <- OptionT.liftF(executor.waitTo(expFleetPhaseTwo.arrivalTime))
        _ <- OptionT.liftF(notifier.notify(Notification.ExpeditionReturned(fleet.from, fleet.fleetId)))
      } yield ()).getOrElseF(notifier.notify(Notification.ExpeditionDestroyed(fleet.from, fleet.fleetId)))
    }
  }

  private def getFleetData(fleet: MyFleet) = {
    ogameActionDriver.readMyFleets().execute().map { myFleetPageData =>
      myFleetPageData.fleets.find(_.fleetId == fleet.fleetId)
    }
  }
}
