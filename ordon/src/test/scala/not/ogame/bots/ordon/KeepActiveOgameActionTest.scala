package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Id
import not.ogame.bots._

import scala.collection.mutable.ListBuffer

class KeepActiveOgameActionTest extends munit.FunSuite {
  private implicit val clock: LocalClock = new RealLocalClock()
  private val planet1: PlayerPlanet = PlayerPlanet(PlanetId("planetId1"), Coordinates(1, 1, 1))
  private val planet2: PlayerPlanet = PlayerPlanet(PlanetId("planetId2"), Coordinates(2, 2, 2))

  test("Should make one planet active") {
    val ogameDriver = new FakeOgameDriver()
    new KeepActiveOgameAction[Id](List(planet1)).processSimple(ogameDriver)
    assert(ogameDriver.readSuppliesPagePlanetId.contains(planet1.id))
  }

  test("Should make all planets active") {
    val ogameDriver = new FakeOgameDriver()
    new KeepActiveOgameAction[Id](List(planet1, planet2)).processSimple(ogameDriver)
    assert(ogameDriver.readSuppliesPagePlanetId.contains(planet1.id))
    assert(ogameDriver.readSuppliesPagePlanetId.contains(planet2.id))
  }
}

class FakeOgameDriver extends OgameDriver[Id]() {
  override def login(): Id[Unit] = ???

  val readSuppliesPagePlanetId: ListBuffer[String] = new ListBuffer[String]()

  override def readSuppliesPage(planetId: PlanetId): Id[SuppliesPageData] = {
    readSuppliesPagePlanetId.addOne(planetId)
    SuppliesPageData(
      ZonedDateTime.now(),
      Resources(0, 0, 0),
      Resources(0, 0, 0),
      Resources(0, 0, 0),
      SuppliesBuildingLevels(Map()),
      Option.empty,
      Option.empty
    )
  }

  override def buildSuppliesBuilding(planetId: PlanetId, suppliesBuilding: SuppliesBuilding): Id[Unit] = ???

  override def readFacilityPage(planetId: PlanetId): Id[FacilityPageData] = ???

  override def buildFacilityBuilding(planetId: PlanetId, facilityBuilding: FacilityBuilding): Id[Unit] = ???

  override def buildShips(planetId: PlanetId, shipType: ShipType, count: Int): Id[Unit] = ???

  override def checkFleetOnPlanet(planetId: PlanetId): Id[Map[ShipType, Int]] = ???

  override def readAllFleets(): Id[List[Fleet]] = ???

  override def readMyFleets(): Id[List[MyFleet]] = ???

  override def sendFleet(sendFleetRequest: SendFleetRequest): Id[Unit] = ???

  override def returnFleet(fleetId: FleetId): Id[Unit] = ???

  override def readPlanets(): Id[List[PlayerPlanet]] = ???

  override def checkIsLoggedIn(): Id[Boolean] = ???
}
