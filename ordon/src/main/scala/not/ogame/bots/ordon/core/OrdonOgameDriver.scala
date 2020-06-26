package not.ogame.bots.ordon.core

import cats.Id
import cats.implicits._
import not.ogame.bots._

class OrdonOgameDriver(val ogameDriver: OgameDriver[Id]) {
  def login(): Unit =
    ogameDriver.login()

  def readSuppliesPage(planetId: PlanetId): SuppliesPageData =
    ogameDriver.readSuppliesPage(planetId)

  def buildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding): Unit =
    ogameDriver.buildSuppliesBuilding(planetId, suppliesBuilding)

  def readFacilityPage(planetId: PlanetId): FacilityPageData =
    ogameDriver.readFacilityPage(planetId)

  def buildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding): Unit =
    ogameDriver.buildFacilityBuilding(planetId, facilityBuilding)

  def readTechnologyPage(planetId: PlanetId): TechnologyPageData =
    ogameDriver.readTechnologyPage(planetId)

  def startResearch(planetId: PlanetId, technology: Technology): Unit =
    ogameDriver.startResearch(planetId, technology)

  def buildShips(planetId: PlanetId, shipType: ShipType, count: Int): Unit =
    ogameDriver.buildShips(planetId, shipType, count)

  def buildSolarSatellites(planetId: PlanetId, count: Int): Unit =
    ogameDriver.buildSolarSatellites(planetId, count)

  def readFleetPage(planetId: PlanetId): FleetPageData =
    ogameDriver.readFleetPage(planetId)

  def readAllFleets(): List[Fleet] =
    ogameDriver.readAllFleets()

  def readMyFleets(): MyFleetPageData =
    ogameDriver.readMyFleets()

  def sendFleet(sendFleetRequest: SendFleetRequest): Unit =
    ogameDriver.sendFleet(sendFleetRequest)

  def returnFleet(fleetId: FleetId): Unit =
    ogameDriver.returnFleet(fleetId)

  def readPlanets(): List[PlayerPlanet] =
    ogameDriver.readPlanets()

  def checkIsLoggedIn(): Boolean =
    ogameDriver.checkIsLoggedIn()

  def readMyOffers(): List[MyOffer] =
    ogameDriver.readMyOffers()

  def createOffer(planetId: PlanetId, newOffer: MyOffer): Unit =
    ogameDriver.createOffer(planetId, newOffer)
}
