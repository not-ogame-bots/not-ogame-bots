package not.ogame.bots.ordon.core

import cats.effect.IO
import cats.implicits._
import not.ogame.bots._
import not.ogame.bots.selenium.{CouldNotProceedToUrl, TimeoutWaitingForElement}
import org.openqa.selenium.StaleElementReferenceException

class OrdonOgameDriver(val ogameDriver: OgameDriver[IO]) {
  def login(): Unit =
    retryCommonErrors(() => ogameDriver.login())

  def readSuppliesPage(planetId: PlanetId): SuppliesPageData =
    retryCommonErrors(() => ogameDriver.readSuppliesPage(planetId))

  def buildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding): Unit =
    retryCommonErrors(() => ogameDriver.buildSuppliesBuilding(planetId, suppliesBuilding))

  def readFacilityPage(planetId: PlanetId): FacilityPageData =
    retryCommonErrors(() => ogameDriver.readFacilityPage(planetId))

  def buildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding): Unit =
    retryCommonErrors(() => ogameDriver.buildFacilityBuilding(planetId, facilityBuilding))

  def readTechnologyPage(planetId: PlanetId): TechnologyPageData =
    retryCommonErrors(() => ogameDriver.readTechnologyPage(planetId))

  def startResearch(planetId: PlanetId, technology: Technology): Unit =
    retryCommonErrors(() => ogameDriver.startResearch(planetId, technology))

  def buildShips(planetId: PlanetId, shipType: ShipType, count: Int): Unit =
    retryCommonErrors(() => ogameDriver.buildShips(planetId, shipType, count))

  def buildSolarSatellites(planetId: PlanetId, count: Int): Unit =
    retryCommonErrors(() => ogameDriver.buildSolarSatellites(planetId, count))

  def readFleetPage(planetId: PlanetId): FleetPageData =
    retryCommonErrors(() => ogameDriver.readFleetPage(planetId))

  def readAllFleets(): List[Fleet] =
    retryCommonErrors(() => ogameDriver.readAllFleets())

  def readMyFleets(): MyFleetPageData =
    retryCommonErrors(() => ogameDriver.readMyFleets())

  def sendFleet(sendFleetRequest: SendFleetRequest): Unit =
    retryCommonErrors(() => ogameDriver.sendFleet(sendFleetRequest))

  def returnFleet(fleetId: FleetId): Unit =
    retryCommonErrors(() => ogameDriver.returnFleet(fleetId))

  def readPlanets(): List[PlayerPlanet] =
    retryCommonErrors(() => ogameDriver.readPlanets())

  def checkIsLoggedIn(): Boolean =
    retryCommonErrors(() => ogameDriver.checkIsLoggedIn())

  def readMyOffers(): List[MyOffer] =
    retryCommonErrors(() => ogameDriver.readMyOffers())

  def createOffer(planetId: PlanetId, newOffer: MyOffer): Unit =
    retryCommonErrors(() => ogameDriver.createOffer(planetId, newOffer))

  def readGalaxyPage(planetId: PlanetId, galaxy: Int, system: Int): GalaxyPageData =
    retryCommonErrors(() => ogameDriver.readGalaxyPage(planetId, galaxy, system))

  def readAllianceMessages(): List[ChatMessage] =
    retryCommonErrors(() => ogameDriver.readAllianceMessages())

  def readChatConversations(): List[ChatConversations] =
    retryCommonErrors(() => ogameDriver.readChatConversations())

  @scala.annotation.tailrec
  private def retryCommonErrors[T](action: () => IO[T], attempts: Int = 4): T = {
    try {
      action().unsafeRunSync()
    } catch {
      case e: StaleElementReferenceException =>
        if (attempts > 0) {
          e.printStackTrace()
          retryCommonErrors(action, attempts - 1)
        } else {
          throw e
        }
      case e: TimeoutWaitingForElement =>
        if (attempts > 0) {
          e.printStackTrace()
          retryCommonErrors(action, attempts - 1)
        } else {
          throw e
        }
      case e: CouldNotProceedToUrl =>
        if (attempts > 0) {
          e.printStackTrace()
          ogameDriver.login().unsafeRunSync()
          retryCommonErrors(action, attempts - 1)
        } else {
          throw e
        }
    }
  }
}
