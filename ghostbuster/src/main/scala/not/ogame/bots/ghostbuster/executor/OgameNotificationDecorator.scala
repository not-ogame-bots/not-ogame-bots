package not.ogame.bots.ghostbuster.executor

import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.subjects.ConcurrentSubject
import not.ogame.bots._
import not.ogame.bots.ghostbuster.FLogger

class OgameNotificationDecorator(driver: OgameDriver[Task])(implicit s: Scheduler)
    extends OgameDriver[Task]
    with NotificationAware
    with FLogger {
  private val notifications = ConcurrentSubject.publish[Notification]

  override def login(): Task[Unit] = logStartEnd("login") {
    driver.login().flatTap(_ => notify(Notification.Login()))
  }

  override def readSuppliesPage(planetId: PlanetId): Task[SuppliesPageData] = logStartEnd("readSupplies") {
    driver
      .readSuppliesPage(planetId)
      .flatTap(sp => notify(Notification.SuppliesPageDateRefreshed(sp, planetId)))
      .onError { case e => notify(Notification.Failure(e)) }
  }

  override def buildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding): Task[Unit] = logStartEnd("buildSupplies") {
    driver
      .buildSuppliesBuilding(planetId, suppliesBuilding)
      .flatTap(_ => notify(Notification.SupplyBuilt(suppliesBuilding)))
      .onError { case e => notify(Notification.Failure(e)) }
  }

  override def readFacilityPage(planetId: PlanetId): Task[FacilityPageData] = logStartEnd("readFacilityPage") {
    driver
      .readFacilityPage(planetId)
      .flatTap(fp => notify(Notification.FacilityPageDataRefreshed(fp, planetId)))
      .onError { case e => notify(Notification.Failure(e)) }
  }

  override def buildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding): Task[Unit] = logStartEnd("buildFacility") {
    driver
      .buildFacilityBuilding(planetId, facilityBuilding)
      .flatTap(_ => notify(Notification.FacilityBuilt(facilityBuilding)))
      .onError { case e => notify(Notification.Failure(e)) }
  }

  override def buildShips(planetId: PlanetId, shipType: ShipType, count: Int): Task[Unit] = logStartEnd("buildShips") {
    driver
      .buildShips(planetId, shipType, count)
      .flatTap(_ => notify(Notification.ShipBuilt(shipType, count, planetId)))
      .onError { case e => notify(Notification.Failure(e)) }
  }

  override def readFleetPage(planetId: PlanetId): Task[FleetPageData] = logStartEnd("readFleetPage") {
    driver
      .readFleetPage(planetId)
      .flatTap(fp => notify(Notification.FleetOnPlanetRefreshed(fp, planetId)))
      .onError { case e => notify(Notification.Failure(e)) }
  }

  override def readAllFleets(): Task[List[Fleet]] = logStartEnd("readAllFleets") {
    driver
      .readAllFleets()
      .flatTap(f => notify(Notification.ReadAllFleets(f)))
      .onError { case e => notify(Notification.Failure(e)) }
  }
  override def readMyFleets(): Task[MyFleetPageData] = logStartEnd("readMyFleets") {
    driver
      .readMyFleets()
      .flatTap(f => notify(Notification.ReadMyFleetAction(f)))
      .onError { case e => notify(Notification.Failure(e)) }
  }

  override def sendFleet(sendFleetRequest: SendFleetRequest): Task[Unit] = logStartEnd("sendFleet") {
    driver
      .sendFleet(sendFleetRequest)
      .flatTap(_ => notify(Notification.FleetSent(sendFleetRequest)))
      .onError { case e => notify(Notification.Failure(e)) }
  }

  override def returnFleet(fleetId: FleetId): Task[Unit] = logStartEnd("returnFleet") {
    driver
      .returnFleet(fleetId)
      .flatTap(_ => notify(Notification.FleetReturned(fleetId)))
      .onError { case e => notify(Notification.Failure(e)) }
  }

  override def readPlanets(): Task[List[PlayerPlanet]] = logStartEnd("readPlanets") {
    driver
      .readPlanets()
      .flatTap(p => notify(Notification.ReadPlanets(p)))
      .onError { case e => notify(Notification.Failure(e)) }
  }

  override def checkIsLoggedIn(): Task[Boolean] = logStartEnd("checkIsLogged") {
    driver.checkIsLoggedIn()
  }

  private def notify(refreshed: Notification) = {
    Task.fromFuture(notifications.onNext(refreshed)).void
  }

  private def logStartEnd[T](message: String)(action: Task[T]): Task[T] = {
    Logger[Task].debug(s"start $message") >> action <* Logger[Task].debug(s"end $message")
  }

  override def subscribeToNotifications: Observable[Notification] = notifications

  override def readMyOffers(): Task[List[MyOffer]] = ???

  override def createOffer(planetId: PlanetId, newOffer: MyOffer): Task[Unit] = ???

  override def readTechnologyPage(planetId: PlanetId): Task[TechnologyPageData] = logStartEnd("readTechnology") {
    driver.readTechnologyPage(planetId).flatTap(tp => notify(Notification.TechnologyPageDataRefreshed(tp, planetId)))
  }
}

trait NotificationAware {
  def subscribeToNotifications: Observable[Notification]
}
