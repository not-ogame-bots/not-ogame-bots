package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.FleetMissionType.Deployment
import not.ogame.bots.ShipType.SmallCargoShip
import not.ogame.bots._
import not.ogame.bots.ordon.utils.SendFleet

import scala.util.Random

class FsBattleProcess[T[_]: Monad](config: BattleProcessConfig)(implicit clock: LocalClock) {
  def startAction(): OgameAction[T] = new InitialAction()

  class InitialAction extends OgameAction[T] {
    override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] = {
      ogame.readMyFleets().map(fleets => handleFleets(fleets)).map(action => List(action))
    }

    private def handleFleets(fleets: List[MyFleet]): ScheduledAction[T] = {
      val maybeFleet = fleets.find(fleet => isThisFleet(fleet))
      if (maybeFleet.isDefined) {
        val thisFleet = maybeFleet.get
        if (thisFleet.isReturning) {
          ScheduledAction(thisFleet.arrivalTime.plusSeconds(3), new HandleFleetOnFsPlanet())
        } else if (isCloseToArrival(thisFleet)) {
          ScheduledAction(clock.now(), new ReturnFleet())
        } else {
          ScheduledAction(chooseTimeWhenClickReturn(thisFleet), new ReturnFleet())
        }
      } else {
        ScheduledAction(clock.now(), new HandleFleetOnFsPlanet())
      }
    }
  }

  private def isThisFleet(fleet: MyFleet): Boolean = {
    fleet.from == config.fsPlanet.coordinates &&
    fleet.to == config.fsMoon.coordinates &&
    fleet.fleetMissionType == Deployment
  }

  private def isCloseToArrival(fleet: MyFleet) = {
    fleet.arrivalTime
      .minusMinutes(config.safeBufferInMinutes)
      .minusSeconds(config.randomUpperLimitInSeconds)
      .isBefore(clock.now())
  }

  private def chooseTimeWhenClickReturn(fleet: MyFleet): ZonedDateTime = {
    val arrivalTime = fleet.arrivalTime
    chooseTimeWhenClickReturn(arrivalTime)
  }

  private def chooseTimeWhenClickReturn(arrivalTime: ZonedDateTime): ZonedDateTime = {
    arrivalTime
      .minusMinutes(config.safeBufferInMinutes)
      .minusSeconds(Random.nextLong(config.randomUpperLimitInSeconds))
  }

  class ReturnFleet extends SimpleOgameAction[T] {
    override def nextAction: OgameAction[T] = new HandleFleetOnFsPlanet()

    override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] =
      for {
        myFleets <- ogame.readMyFleets()
        thisFleet = myFleets.filter(fleet => isThisFleet(fleet)).head
        _ <- ogame.returnFleet(thisFleet.fleetId)
        myFleetsAfterReturn <- ogame.readMyFleets()
        thisFleetReturning = myFleetsAfterReturn.filter(fleet => isThisFleet(fleet)).head
      } yield thisFleetReturning.arrivalTime.plusSeconds(3)
  }

  class HandleFleetOnFsPlanet extends OgameAction[T] {
    override def process(ogame: OgameDriver[T]): T[List[ScheduledAction[T]]] =
      List(new SendMissingSmallCargoToExpeditionMoon(), new SendFleetToFsMoon())
        .map(action => ScheduledAction(clock.now(), action))
        .pure[T]
  }

  class SendFleetToFsMoon extends SimpleOgameAction[T] {
    val sendFleet = new SendFleet(from = config.fsPlanet, to = config.fsMoon, fleetSpeed = FleetSpeed.Percent10)

    override def nextAction: OgameAction[T] = new ReturnFleet()

    override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] =
      sendFleet
        .sendDeployment(ogame)
        .map(arrivalTime => chooseTimeWhenClickReturn(arrivalTime))
  }

  class SendMissingSmallCargoToExpeditionMoon extends SimpleOgameAction[T] {
    override def nextAction: OgameAction[T] = new EndAction[T]

    def sendFleet(smallCargoToSend: Int) =
      new SendFleet(
        from = config.fsPlanet,
        to = config.expeditionMoon,
        selectShips = _ => Map((SmallCargoShip, smallCargoToSend)),
        selectResources = _ => Resources(0, 0, 0)
      )

    override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] =
      for {
        myFleets <- ogame.readMyFleets()
        cargoFleets = myFleets.filter(fleet => isCargoFleet(fleet))
        flyingCargoShips = cargoFleets.map(fleet => fleet.ships(SmallCargoShip)).sum
        shouldSendMissingCargoShips = cargoFleets.nonEmpty && flyingCargoShips < 20_000
        endTime <- if (shouldSendMissingCargoShips) sendFleet(20_000 - flyingCargoShips).sendDeployment(ogame) else clock.now().pure[T]
      } yield endTime

    private def isCargoFleet(fleet: MyFleet): Boolean = {
      val isCargoPath = isBetweenExpeditionMoonAndOtherMoon(fleet) || isFormFsPlanetToExpeditionMoon(fleet)
      isCargoPath && fleet.fleetMissionType == Deployment
    }
  }

  private def isBetweenExpeditionMoonAndOtherMoon(fleet: MyFleet): Boolean = {
    Set(fleet.from, fleet.to) == Set(config.expeditionMoon.coordinates, config.otherMoon.coordinates)
  }

  private def isFormFsPlanetToExpeditionMoon(fleet: MyFleet): Boolean = {
    fleet.from == config.fsPlanet.coordinates && fleet.to == config.expeditionMoon.coordinates
  }
}

trait BattleProcessConfig {
  val fsPlanet: PlayerPlanet
  val fsMoon: PlayerPlanet
  val expeditionMoon: PlayerPlanet
  val otherMoon: PlayerPlanet
  val safeBufferInMinutes: Int
  val randomUpperLimitInSeconds: Int
}
