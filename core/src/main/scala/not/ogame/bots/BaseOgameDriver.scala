package not.ogame.bots

abstract class BaseOgameDriver[F[_]] extends OgameDriver[F] {
  override def login(): F[Unit] = ???

  override def readSuppliesPage(planetId: PlanetId): F[SuppliesPageData] = ???

  override def buildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding): F[Unit] = ???

  override def readFacilityPage(planetId: PlanetId): F[FacilityPageData] = ???

  override def buildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding): F[Unit] = ???

  override def readTechnologyPage(planetId: PlanetId): F[TechnologyPageData] = ???

  override def startResearch(planetId: PlanetId, technology: Technology): F[Unit] = ???

  override def buildShips(planetId: PlanetId, shipType: ShipType, count: Int): F[Unit] = ???

  override def buildSolarSatellites(planetId: PlanetId, count: Int): F[Unit] = ???

  override def readFleetPage(planetId: PlanetId): F[FleetPageData] = ???

  override def readAllFleets(): F[List[Fleet]] = ???

  override def readMyFleets(): F[MyFleetPageData] = ???

  override def sendFleet(sendFleetRequest: SendFleetRequest): F[Unit] = ???

  override def returnFleet(fleetId: FleetId): F[Unit] = ???

  override def readPlanets(): F[List[PlayerPlanet]] = ???

  override def checkIsLoggedIn(): F[Boolean] = ???

  override def readMyOffers(): F[List[MyOffer]] = ???

  override def createOffer(planetId: PlanetId, newOffer: MyOffer): F[Unit] = ???

  override def readGalaxyPage(planetId: PlanetId, galaxy: Int, system: Int): F[GalaxyPageData] = ???
}
