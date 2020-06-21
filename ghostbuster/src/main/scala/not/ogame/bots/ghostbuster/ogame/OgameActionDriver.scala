package not.ogame.bots.ghostbuster.ogame

import cats.free.Free.liftF
import not.ogame.bots._

class OgameActionDriver extends OgameDriver[OgameAction] {
  def readSuppliesPage(planetId: PlanetId): OgameAction[SuppliesPageData] =
    liftF[OgameOp, SuppliesPageData](OgameOp.ReadSupplyPage(planetId))
  def readTechnologyPage(planetId: PlanetId): OgameAction[TechnologyPageData] =
    liftF[OgameOp, TechnologyPageData](OgameOp.ReadTechnologyPage(planetId))
  def readFacilityPage(planetId: PlanetId): OgameAction[FacilityPageData] =
    liftF[OgameOp, FacilityPageData](OgameOp.ReadFacilityPage(planetId))
  def readFleetPage(planetId: PlanetId): OgameAction[FleetPageData] = liftF[OgameOp, FleetPageData](OgameOp.ReadFleetPage(planetId))
  def readAllFleets(): OgameAction[List[Fleet]] = liftF[OgameOp, List[Fleet]](OgameOp.ReadAllFleets())
  def readMyFleets(): OgameAction[MyFleetPageData] = liftF[OgameOp, MyFleetPageData](OgameOp.ReadMyFleets())
  def readPlanets(): OgameAction[List[PlayerPlanet]] = liftF[OgameOp, List[PlayerPlanet]](OgameOp.ReadPlanets())
  def checkIsLoggedIn(): OgameAction[Boolean] = liftF[OgameOp, Boolean](OgameOp.CheckLoginStatus())
  def readMyOffers(): OgameAction[List[MyOffer]] = liftF[OgameOp, List[MyOffer]](OgameOp.ReadMyOffers())

  def login(): OgameAction[Unit] = liftF[OgameOp, Unit](OgameOp.Login())
  def buildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding): OgameAction[Unit] =
    liftF[OgameOp, Unit](OgameOp.BuildSuppliesBuilding(planetId, suppliesBuilding))
  def buildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding): OgameAction[Unit] =
    liftF[OgameOp, Unit](OgameOp.BuildFacilityBuilding(planetId, facilityBuilding))
  def startResearch(planetId: PlanetId, technology: Technology): OgameAction[Unit] =
    liftF[OgameOp, Unit](OgameOp.StartResearch(planetId, technology))
  def buildShips(planetId: PlanetId, shipType: ShipType, count: Int): OgameAction[Unit] =
    liftF[OgameOp, Unit](OgameOp.BuildShip(planetId, shipType, count))
  def sendFleet(sendFleetRequest: SendFleetRequest): OgameAction[Unit] =
    liftF[OgameOp, Unit](OgameOp.SendFleet(sendFleetRequest))
  def returnFleet(fleetId: FleetId): OgameAction[Unit] =
    liftF[OgameOp, Unit](OgameOp.ReturnFleet(fleetId))
  def createOffer(planetId: PlanetId, newOffer: MyOffer): OgameAction[Unit] =
    liftF[OgameOp, Unit](OgameOp.CreateOffer(planetId, newOffer))
  def buildSolarSatellites(planetId: PlanetId, count: Int): OgameAction[Unit] =
    liftF[OgameOp, Unit](OgameOp.BuildSolarSatellite(planetId, count))
}
