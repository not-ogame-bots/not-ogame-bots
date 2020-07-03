package not.ogame.bots.ghostbuster.processors

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.reactive.Consumer
import not.ogame.bots._
import not.ogame.bots.ghostbuster.executor.{OgameActionExecutor, _}
import not.ogame.bots.ghostbuster.notifications.Notification
import not.ogame.bots.ghostbuster.ogame.OgameAction
import not.ogame.bots.ghostbuster.{ExpeditionConfig, ExpeditionDebrisCollectorConfig, FLogger}

class ExpeditionDebrisCollectingProcessor(
    driver: OgameDriver[OgameAction],
    config: ExpeditionDebrisCollectorConfig,
    expeditionConfig: ExpeditionConfig
)(
    implicit executor: OgameActionExecutor[Task]
) extends FLogger {
  def run(): Task[Unit] = {
    driver
      .readPlanets()
      .execute()
      .flatMap { planets =>
        withRetry(loop(planets.filter(_.id == config.from).head))("expeditionDebrisCollector")
      }
  }

  private val debrisTarget: Coordinates = expeditionConfig.target.copy(coordinatesType = CoordinatesType.Debris)

  private def loop(from: PlayerPlanet) = {
    executor.subscribeToNotifications
      .collect { case n: Notification.ExpeditionTick => n }
      .consumeWith(Consumer.foreachTask {
        _ =>
          driver
            .readGalaxyPage(planetId = config.from, expeditionConfig.target.galaxy, expeditionConfig.target.system)
            .flatMap { gp =>
              if (gp.debrisMap.contains(debrisTarget)) {
                Logger[OgameAction].info("Found some debris from expedition") >>
                  driver
                    .readMyFleets()
                    .flatMap { fleetPage =>
                      val collectorsAlreadyOnTheMove =
                        fleetPage.fleets.exists(f => f.fleetMissionType == FleetMissionType.Recycle && f.to == debrisTarget)
                      if (collectorsAlreadyOnTheMove) {
                        Logger[OgameAction].info("Collecting fleet is in the air")
                      } else {
                        trySendingFleet(from, fleetPage)
                      }
                    }
              } else {
                Logger[OgameAction].info("No debris from expedition...")
              }
            }
            .execute()
      })
  }

  private def trySendingFleet(from: PlayerPlanet, fleetPage: MyFleetPageData) = {
    if (fleetPage.fleetSlots.isAtLeastOneSlotAvailable) {
      Logger[OgameAction].info("Sending explorers to collect") >>
        driver.readFleetPage(config.from).flatMap { fp =>
          val explorersAmount = fp.ships(ShipType.Explorer)
          if (explorersAmount > 0) {
            driver.sendFleet(
              SendFleetRequest(
                from,
                SendFleetRequestShips.Ships(Map(ShipType.Explorer -> fp.ships(ShipType.Explorer))),
                debrisTarget,
                FleetMissionType.Recycle,
                FleetResources.Given(Resources.Zero)
              )
            )
          } else {
            Logger[OgameAction].error("No explorers on configured planet...")
          }
        }
    } else {
      Logger[OgameAction].error("Not enough slots to collect debris...")
    }
  }
}
