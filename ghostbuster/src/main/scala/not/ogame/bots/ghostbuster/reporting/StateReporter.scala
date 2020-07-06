package not.ogame.bots.ghostbuster.reporting

import monix.eval.Task
import monix.reactive.{Consumer, Observable}
import not.ogame.bots.ghostbuster.infrastructure.{Channel, SlackService}
import not.ogame.bots.ghostbuster.notifications.{Notification, Notifier}
import not.ogame.bots.ghostbuster.processors.{ExpeditionConfig, FlyAndReturnConfig, FsConfig}
import not.ogame.bots.{FleetAttitude, FleetMissionType, PlayerPlanet}

import scala.concurrent.duration._

class StateReporter(
    slackService: SlackService[Task],
    notifier: Notifier,
    expeditionConfig: ExpeditionConfig,
    fsConfig: FsConfig,
    flyAndReturnConfig: FlyAndReturnConfig
) {
  def run(): Task[Unit] = {
    waitForPlanets
      .flatMap { planets =>
        Observable
          .combineLatestMap4(flyAndReturnFs(planets), fsFleet(planets), expeditions, hostileFleets)((a, b, c, d) => List(a, b, c, d))
          .throttleLast(5 minutes)
          .map { reports =>
            reports
              .map {
                case Left(value)  => s"X $value"
                case Right(value) => s"OK $value"
              }
              .mkString("\n")
          }
          .consumeWith(Consumer.foreachTask(slackService.postMessage(_, Channel.Status)))
      }
  }

  private def waitForPlanets = {
    notifier.subscribeToNotifications
      .collect {
        case Notification.ReadPlanets(planets) => planets
      }
      .consumeWith(Consumer.head)
  }

  private def flyAndReturnFs(planets: List[PlayerPlanet]) = {
    val from = planets.find(_.id == flyAndReturnConfig.from).get
    val to = planets.find(_.id == flyAndReturnConfig.to).get
    notifier.subscribeToNotifications
      .collect {
        case Notification.ReadAllFleets(fleets) =>
          fleets.exists(f => f.from == from.coordinates && f.to == to.coordinates && f.fleetMissionType == FleetMissionType.Deployment)
        case Notification.ReadMyFleetAction(myFleetPageData) =>
          myFleetPageData.fleets.exists(
            f => f.from == from.coordinates && f.to == to.coordinates && f.fleetMissionType == FleetMissionType.Deployment
          )
      }
      .map { fleetDetected =>
        if (fleetDetected) {
          Right("Military fs fleet is flying")
        } else {
          Left("Military fs fleet is not flying")
        }
      }
  }

  private def fsFleet(planets: List[PlayerPlanet]) = {
    val expectedFsShips = fsConfig.ships.map(fs => fs.shipType -> fs.amount).toMap
    val eligiblePlanets = fsConfig.eligiblePlanets.map(pId => planets.find(_.id == pId).get)
    notifier.subscribeToNotifications
      .collect {
        case Notification.ReadMyFleetAction(myFleetPageData) =>
          val fsFleetDetected = myFleetPageData.fleets.exists { f =>
            eligiblePlanets.exists(_.coordinates == f.from) && eligiblePlanets.exists(_.coordinates == f.to) &&
            f.fleetMissionType == FleetMissionType.Deployment && f.ships.forall {
              case (shipType, amount) => amount >= expectedFsShips(shipType)
            }
          }
          if (fsFleetDetected) {
            Right("FS fleet is flying")
          } else {
            Left("FS fleet is not flying")
          }
      }
  }

  private def expeditions = {
    notifier.subscribeToNotifications
      .collect {
        case Notification.ReadAllFleets(fleets) => fleets.filter(_.fleetMissionType == FleetMissionType.Expedition).count(_.isReturning)
        case Notification.ReadMyFleetAction(myFleetPageData) =>
          myFleetPageData.fleets.count(_.fleetMissionType == FleetMissionType.Expedition)
      }
      .map { expeditionsCount =>
        val message = s"Expeditions fleet count: $expeditionsCount / ${expeditionConfig.maxNumberOfExpeditions}"
        if (expeditionsCount < expeditionConfig.maxNumberOfExpeditions) {
          Left(message)
        } else {
          Right(message)
        }
      }
  }

  private def hostileFleets = {
    notifier.subscribeToNotifications
      .collect { case Notification.ReadAllFleets(fleets) => fleets }
      .map { fleets =>
        val hostileFleets = fleets.filter { f =>
          f.fleetAttitude == FleetAttitude.Hostile && (f.fleetMissionType == FleetMissionType.Attack || f.fleetMissionType == FleetMissionType.Destroy)
        }
        if (hostileFleets.isEmpty) {
          Right("No hostile fleet detected")
        } else {
          Left(s"Hostile fleet detected!! First will arrive at ${hostileFleets.minBy(_.arrivalTime)}")
        }
      }
  }
}
