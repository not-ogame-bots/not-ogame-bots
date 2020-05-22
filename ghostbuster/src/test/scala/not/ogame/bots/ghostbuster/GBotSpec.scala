package not.ogame.bots.ghostbuster

import java.time.{Clock, Instant, ZoneId}

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.NonNegative
import not.ogame.bots._

class GBotSpec extends munit.FunSuite {
  private val now = Instant.now()
  private val unused = now
  private implicit val clock: Clock = Clock.fixed(now, ZoneId.systemDefault())
  private val randomTimeJitter: RandomTimeJitter = () => 0
  private val bigCapacity = Resources(10000, 10000, 10000, 0)

  test("should do nothing if wishlist is empty") {
    val bot = new GBot(randomTimeJitter, BotConfig(List.empty, buildMtUpToCapacity = false))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(1, 1, 1),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty,
        Option.empty
      ),
      List.empty,
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState, prevState)
  }

  test("should refresh fleet state on planet if wishlist is empty") {
    val bot = new GBot(randomTimeJitter, BotConfig(List.empty, buildMtUpToCapacity = true))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(1, 1, 1),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty,
        Option.empty
      ),
      List.empty,
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.RefreshFleetOnPlanetStatus(ShipType.SMALL_CARGO_SHIP, clock.instant())))
  }

  test("should schedule building metal factory now if there is enough resources") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.buildSupply(SuppliesBuilding.MetalMine, 1)), buildMtUpToCapacity = false))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(60, 15, 0),
        Resources(0, 0, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty,
        Option.empty
      ),
      List.empty,
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.buildSupply(SuppliesBuilding.MetalMine, 1, now)))
  }

  test("should remove building from wishlist if it is already built") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.buildSupply(SuppliesBuilding.MetalMine, 1)), buildMtUpToCapacity = false))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(60, 15, 0),
        Resources(0, 0, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 1)),
        Option.empty,
        Option.empty
      ),
      List.empty,
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List.empty)
  }

  test("should schedule building metal factory now if there is enough resources - with a jump") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.buildSupply(SuppliesBuilding.MetalMine, 10)), buildMtUpToCapacity = false))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(60, 15, 0),
        Resources(0, 0, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty,
        Option.empty
      ),
      List.empty,
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.buildSupply(SuppliesBuilding.MetalMine, 1, now)))
  }

  test("should not schedule building metal factory if there is something scheduled") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.buildSupply(SuppliesBuilding.MetalMine, 1)), buildMtUpToCapacity = false))

    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(60, 15, 0),
        Resources(0, 0, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty,
        Option.empty
      ),
      List(Task.buildSupply(SuppliesBuilding.CrystalMine, 1, now)),
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.BuildSupply(SuppliesBuilding.CrystalMine, 1, now)))
  }
  test("should schedule building metal factory in the future if there is not enough resources") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.buildSupply(SuppliesBuilding.MetalMine, 1)), buildMtUpToCapacity = false))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(10, 10, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty,
        Option.empty
      ),
      List.empty,
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.buildSupply(SuppliesBuilding.MetalMine, 1, now.plusSeconds(6 * 3600))))
  }

  test("should schedule building metal factory in the future if there is not enough resources - with a jump") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.buildSupply(SuppliesBuilding.MetalMine, 10)), buildMtUpToCapacity = false))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(10, 10, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty,
        Option.empty
      ),
      List.empty,
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.buildSupply(SuppliesBuilding.MetalMine, 1, now.plusSeconds(6 * 3600))))
  }

  test("should not build building if it is already built") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.buildSupply(SuppliesBuilding.MetalMine, 1)), buildMtUpToCapacity = false))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(10, 10, 10),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 1)),
        Option.empty,
        Option.empty
      ),
      List.empty,
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List.empty)
  }

  test("should schedule refresh after building finishes") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.buildSupply(SuppliesBuilding.MetalMine, 1)), buildMtUpToCapacity = false))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(10, 10, 10),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 1)),
        Some(BuildingProgress(now.plusSeconds(1))),
        Option.empty
      ),
      List.empty,
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.refreshSupplyPage(now.plusSeconds(1))))
  }

  test("should build storage if there is not enough capacity") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.buildSupply(SuppliesBuilding.MetalMine, 11)), buildMtUpToCapacity = false))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(100, 0, 0),
        Resources(1500, 0, 0),
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 10)),
        Option.empty,
        Option.empty
      ),
      List.empty,
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.buildSupply(SuppliesBuilding.MetalStorage, 1, now.plusSeconds(10 * 3600))))
  }

  test("should build power plant if there is not enough energy") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.buildSupply(SuppliesBuilding.MetalMine, 11)), buildMtUpToCapacity = false))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0, -100),
        Resources(75, 30, 0),
        Resources(2000, 2000, 0),
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 10)),
        Option.empty,
        Option.empty
      ),
      List.empty,
      createFacilityBuildings,
      Map.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.buildSupply(SuppliesBuilding.SolarPlant, 1, now.plusSeconds(1 * 3600))))
  }

  private def createStartingBuildings: Map[SuppliesBuilding, Int Refined NonNegative] = {
    SuppliesBuilding.values.map(_ -> refineMV[NonNegative](0)).toMap
  }

  private def createFacilityBuildings: FacilitiesBuildingLevels = {
    FacilitiesBuildingLevels(FacilityBuilding.values.map(_ -> refineMV[NonNegative](0)).toMap)
  }

  //TODO ?? suppliesBuildings map -> case class
  //TODO resources @newtype metal, crystal, deuter
}
