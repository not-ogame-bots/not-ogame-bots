package not.ogame.bots.ghostbuster

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.NonNegative
import not.ogame.bots.{BuildingProgress, Resources, SuppliesBuilding, SuppliesBuildingLevels, SuppliesPageData}

class GBotSpec extends munit.FunSuite {
  private val now = Instant.now()
  private val unused: LocalDateTime = LocalDateTime.now()
  private implicit val clock: Clock = Clock.fixed(now, ZoneId.systemDefault())
  private val randomTimeJitter: RandomTimeJitter = () => 0
  private val bigCapacity = Resources(10000, 10000, 10000, 0)

  test("should do nothing if wishlist is empty") {
    val bot = new GBot(randomTimeJitter, BotConfig(List.empty))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(1, 1, 1),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty
      ),
      List.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState, prevState)
  }
  test("should schedule building metal factory now if there is enough resources") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.build(SuppliesBuilding.MetalMine, 1))))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(60, 15, 0),
        Resources(0, 0, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty
      ),
      List.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.build(SuppliesBuilding.MetalMine, 1, now)))
  }

  test("should remove building from wishlist if it is already built") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.build(SuppliesBuilding.MetalMine, 1))))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(60, 15, 0),
        Resources(0, 0, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 1)),
        Option.empty
      ),
      List.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List.empty)
  }

  test("should schedule building metal factory now if there is enough resources - with a jump") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.build(SuppliesBuilding.MetalMine, 10))))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(60, 15, 0),
        Resources(0, 0, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty
      ),
      List.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.build(SuppliesBuilding.MetalMine, 1, now)))
  }

  test("should not schedule building metal factory if there is something scheduled") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.build(SuppliesBuilding.MetalMine, 1))))

    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(60, 15, 0),
        Resources(0, 0, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty
      ),
      List(Task.build(SuppliesBuilding.CrystalMine, 1, now))
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.BuildSupply(SuppliesBuilding.CrystalMine, 1, now)))
  }
  test("should schedule building metal factory in the future if there is not enough resources") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.build(SuppliesBuilding.MetalMine, 1))))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(10, 10, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty
      ),
      List.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.build(SuppliesBuilding.MetalMine, 1, now.plusSeconds(6 * 3600))))
  }

  test("should schedule building metal factory in the future if there is not enough resources - with a jump") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.build(SuppliesBuilding.MetalMine, 10))))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(10, 10, 0),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty
      ),
      List.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.build(SuppliesBuilding.MetalMine, 1, now.plusSeconds(6 * 3600))))
  }

  test("should not build building if it is already built") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.build(SuppliesBuilding.MetalMine, 1))))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(10, 10, 10),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 1)),
        Option.empty
      ),
      List.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List.empty)
  }

  test("should schedule refresh after building finishes") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.build(SuppliesBuilding.MetalMine, 1))))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(10, 10, 10),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 1)),
        Some(BuildingProgress(now.plusSeconds(1)))
      ),
      List.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.refresh(now.plusSeconds(1))))
  }

  test("should build storage if there is not enough capacity") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.build(SuppliesBuilding.MetalMine, 11))))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(100, 0, 0),
        Resources(1500, 0, 0),
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 10)),
        Option.empty
      ),
      List.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.build(SuppliesBuilding.MetalStorage, 1, now.plusSeconds(10 * 3600))))
  }

  test("should build power plant if there is not enough energy") {
    val bot = new GBot(randomTimeJitter, BotConfig(List(Wish.build(SuppliesBuilding.MetalMine, 11))))
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0, -100),
        Resources(75, 30, 0),
        Resources(2000, 2000, 0),
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 10)),
        Option.empty
      ),
      List.empty
    )
    val nextState: PlanetState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.build(SuppliesBuilding.SolarPlant, 1, now.plusSeconds(1 * 3600))))
  }

  private def createStartingBuildings: Map[SuppliesBuilding, Int Refined NonNegative] = {
    SuppliesBuilding.values.map(_ -> refineMV[NonNegative](0)).toMap
  }

  //TODO ?? suppliesBuildings map -> case class
  //TODO resources @newtype metal, crystal, deuter
}
