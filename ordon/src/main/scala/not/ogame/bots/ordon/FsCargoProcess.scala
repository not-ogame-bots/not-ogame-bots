package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots.ShipType._
import not.ogame.bots._
import not.ogame.bots.ordon.utils._

class FsCargoProcess[T[_]: Monad](cargoProcessConfig: CargoProcessConfig)(implicit clock: LocalClock) {
  def startAction(): OgameAction[T] = new InitialAction()

  class InitialAction extends OgameAction[T] {
    override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] = {
      ogame.readAllFleets().flatMap(fleets => handleFleets(ogame, fleets))
    }

    private def handleFleets(ogame: OgameDriver[T], fleets: List[Fleet]): T[List[ScheduledAction[T]]] = {
      val maybeFleet = fleets.find(fleet => isThisFleet(fleet))
      if (maybeFleet.isDefined) {
        List(ScheduledAction(maybeFleet.get.arrivalTime.plusSeconds(3), this)).pure[T]
      } else {
        chooseStaringPlanet(ogame)
      }
    }

    private def isThisFleet(fleet: Fleet): Boolean = {
      val isFlyingOnCorrectPath = Set(fleet.from, fleet.to) == Set(
        cargoProcessConfig.expeditionMoon.coordinates,
        cargoProcessConfig.otherMoon.coordinates
      )
      isFlyingOnCorrectPath && fleet.fleetMissionType == Deployment
    }

    private def chooseStaringPlanet(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] =
      for {
        fleetOnOtherMoon <- ogame.readFleetPage(cargoProcessConfig.otherMoon.id)
        isFleetOnOtherMoon = fleetOnOtherMoon.ships.values.sum > 0
        nextActions = if (isFleetOnOtherMoon) {
          List(new SendFleetFromOtherMoon())
        } else {
          List(new HandleFleetOnExpeditionMoon())
        }
      } yield nextActions.map(action => ScheduledAction(clock.now(), action))
  }

  class HandleFleetOnExpeditionMoon extends OgameAction[T] {
    override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] =
      List(
        ScheduledAction(clock.now(), new SendFleetToFsPlanet()),
        ScheduledAction(clock.now(), new SendFleetFromExpeditionMoon())
      ).pure[T]
  }

  class SendFleetFromOtherMoon extends SimpleOgameAction[T] {
    val sendFleet: SendFleet =
      new SendFleet(cargoProcessConfig.otherMoon, cargoProcessConfig.expeditionMoon, fleetSpeed = FleetSpeed.Percent30)

    override def nextAction: OgameAction[T] = new HandleFleetOnExpeditionMoon()

    override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] = sendFleet.sendDeployment(ogame)
  }

  class SendFleetToFsPlanet extends SimpleOgameAction[T] {
    val resourceSelector = new ResourceSelector(
      deuteriumSelector = Selector.decreaseBy(1_000_000)
    )
    val fleetSelector: SelectShips = (page: FleetPageData) => {
      val smallCargoCount = (page.currentResources.metal + page.currentResources.crystal + page.currentResources.deuterium) / 7_000 + 1
      new FleetSelector(
        filters = Map(
          Destroyer -> Selector.decreaseBy(6),
          EspionageProbe -> Selector.decreaseBy(50),
          Explorer -> Selector.skip,
          LargeCargoShip -> Selector.skip,
          SmallCargoShip -> Selector.atMost(smallCargoCount)
        )
      ).apply(page)
    }
    val sendFleet: SendFleet =
      new SendFleet(
        cargoProcessConfig.expeditionMoon,
        cargoProcessConfig.fsPlanet,
        selectShips = fleetSelector,
        selectResources = resourceSelector
      )

    override def nextAction: OgameAction[T] = new EndAction[T]()

    override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] =
      for {
        page <- ogame.readFleetPage(cargoProcessConfig.expeditionMoon.id)
        isLotsOfResources = (page.currentResources.metal + page.currentResources.crystal + page.currentResources.deuterium) > 10_000 * 7_000
        endTime <- if (isLotsOfResources) sendFleet.sendDeployment(ogame) else clock.now().pure[T]
      } yield endTime
  }

  class SendFleetFromExpeditionMoon extends SimpleOgameAction[T] {
    val resourceSelector = new ResourceSelector(
      deuteriumSelector = Selector.decreaseBy(400_000)
    )
    val fleetSelector = new FleetSelector(
      filters = Map(
        Destroyer -> Selector.decreaseBy(6),
        EspionageProbe -> Selector.decreaseBy(50),
        Explorer -> Selector.skip,
        LargeCargoShip -> Selector.decreaseBy(410)
      )
    )
    val sendFleet: SendFleet =
      new SendFleet(
        cargoProcessConfig.expeditionMoon,
        cargoProcessConfig.otherMoon,
        selectShips = fleetSelector,
        selectResources = resourceSelector,
        fleetSpeed = FleetSpeed.Percent30
      )

    override def nextAction: OgameAction[T] = new SendFleetFromOtherMoon()

    override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] = sendFleet.sendDeployment(ogame)
  }
}

trait CargoProcessConfig {
  val expeditionMoon: PlayerPlanet
  val otherMoon: PlayerPlanet
  val fsPlanet: PlayerPlanet
}
