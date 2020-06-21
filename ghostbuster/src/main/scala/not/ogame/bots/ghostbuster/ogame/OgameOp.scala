package not.ogame.bots.ghostbuster.ogame
import acyclic.skipped
import not.ogame.bots._

sealed trait OgameOp[T]

object OgameOp {
  case class ReadSupplyPage(planetId: PlanetId) extends OgameOp[SuppliesPageData]
  case class ReadTechnologyPage(planetId: PlanetId) extends OgameOp[TechnologyPageData]
  case class ReadFacilityPage(planetId: PlanetId) extends OgameOp[FacilityPageData]
  case class ReadFleetPage(planetId: PlanetId) extends OgameOp[FleetPageData]
  case class ReadAllFleets() extends OgameOp[List[Fleet]]
  case class ReadMyFleets() extends OgameOp[MyFleetPageData]
  case class ReadPlanets() extends OgameOp[List[PlayerPlanet]]
  case class CheckLoginStatus() extends OgameOp[Boolean]
  case class ReadMyOffers() extends OgameOp[List[MyOffer]]

  case class Login() extends OgameOp[Unit]
  case class BuildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding) extends OgameOp[Unit]
  case class BuildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding) extends OgameOp[Unit]
  case class StartResearch(planetId: PlanetId, technology: Technology) extends OgameOp[Unit]
  case class BuildShip(planetId: PlanetId, shipType: ShipType, amount: Int) extends OgameOp[Unit]
  case class SendFleet(request: SendFleetRequest) extends OgameOp[Unit]
  case class ReturnFleet(fleetId: FleetId) extends OgameOp[Unit]
  case class CreateOffer(planetId: PlanetId, newOffer: MyOffer) extends OgameOp[Unit]

  case class RaiseError[A](throwable: Throwable) extends OgameOp[A]
  case class HandleError[A](fa: OgameAction[A], f: Throwable => OgameAction[A]) extends OgameOp[A]
}
