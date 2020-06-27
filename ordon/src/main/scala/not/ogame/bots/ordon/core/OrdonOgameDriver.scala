package not.ogame.bots.ordon.core

import cats.effect.IO
import cats.implicits._
import not.ogame.bots._

class OrdonOgameDriver(val ogameDriver: OgameDriver[IO]) {
  def login(): Unit =
    ogameDriver.login().unsafeRunSync()

  def readSuppliesPage(planetId: PlanetId): SuppliesPageData =
    ogameDriver.readSuppliesPage(planetId).unsafeRunSync()

  def buildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding): Unit =
    ogameDriver.buildSuppliesBuilding(planetId, suppliesBuilding).unsafeRunSync()

  def readFacilityPage(planetId: PlanetId): FacilityPageData =
    ogameDriver.readFacilityPage(planetId).unsafeRunSync()

  def buildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding): Unit =
    ogameDriver.buildFacilityBuilding(planetId, facilityBuilding).unsafeRunSync()

  def readTechnologyPage(planetId: PlanetId): TechnologyPageData =
    ogameDriver.readTechnologyPage(planetId).unsafeRunSync()

  def startResearch(planetId: PlanetId, technology: Technology): Unit =
    ogameDriver.startResearch(planetId, technology).unsafeRunSync()

  def buildShips(planetId: PlanetId, shipType: ShipType, count: Int): Unit =
    ogameDriver.buildShips(planetId, shipType, count).unsafeRunSync()

  def buildSolarSatellites(planetId: PlanetId, count: Int): Unit =
    ogameDriver.buildSolarSatellites(planetId, count).unsafeRunSync()

  def readFleetPage(planetId: PlanetId): FleetPageData =
    ogameDriver.readFleetPage(planetId).unsafeRunSync()

  def readAllFleets(): List[Fleet] =
    ogameDriver.readAllFleets().unsafeRunSync()

  def readMyFleets(): MyFleetPageData =
    ogameDriver.readMyFleets().unsafeRunSync()

  def sendFleet(sendFleetRequest: SendFleetRequest): Unit =
    ogameDriver.sendFleet(sendFleetRequest).unsafeRunSync()

  def returnFleet(fleetId: FleetId): Unit =
    ogameDriver.returnFleet(fleetId).unsafeRunSync()

  def readPlanets(): List[PlayerPlanet] =
    ogameDriver.readPlanets().unsafeRunSync()

  def checkIsLoggedIn(): Boolean =
    ogameDriver.checkIsLoggedIn().unsafeRunSync()

  def readMyOffers(): List[MyOffer] =
    ogameDriver.readMyOffers().unsafeRunSync()

  def createOffer(planetId: PlanetId, newOffer: MyOffer): Unit =
    ogameDriver.createOffer(planetId, newOffer).unsafeRunSync()
}
