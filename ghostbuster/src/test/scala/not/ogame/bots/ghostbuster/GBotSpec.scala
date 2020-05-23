package not.ogame.bots.ghostbuster

import java.time.{Clock, ZoneId}

import eu.timepit.refined.auto._
import not.ogame.bots._

class GBotSpec extends munit.FunSuite {
  private implicit val clock: Clock = Clock.fixed(now, ZoneId.systemDefault())
  private val randomTimeJitter: RandomTimeJitter = () => 0

  test("should do nothing if wishlist is empty") {
    val bot = new GBot(randomTimeJitter, BotConfig(List.empty, buildMtUpToCapacity = false, useWishlist = true, activityFaker = false))
    val prevState = State.LoggedIn(
      List.empty,
      List(createPlanetState(createSuppliesPage())),
      List.empty
    )
    val nextState = bot.nextStep(prevState)
    assertEquals(nextState, prevState)
  }

  test("should refresh fleet state on planet if wishlist is empty") {
    val bot = new GBot(randomTimeJitter, BotConfig(List.empty, buildMtUpToCapacity = true, useWishlist = true, activityFaker = false))
    val prevState = State.LoggedIn(
      List.empty,
      List(createPlanetState(createSuppliesPage())),
      List.empty
    )
    val nextState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.RefreshFleetOnPlanetStatus(ShipType.SmallCargoShip, clock.instant(), PlanetId)))
  }

  test("should schedule building metal factory now if there is enough resources") {
    val bot = new GBot(
      randomTimeJitter,
      BotConfig(
        List(Wish.BuildSupply(SuppliesBuilding.MetalMine, 1, PlanetId)),
        buildMtUpToCapacity = false,
        useWishlist = true,
        activityFaker = false
      )
    )
    val prevState = State.LoggedIn(List.empty, List(createPlanetState(createSuppliesPage(resources = Resources(60, 15, 0)))), List.empty)
    val nextState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.BuildSupply(SuppliesBuilding.MetalMine, 1, now, PlanetId)))
  }

  test("should remove building from wishlist if it is already built") {
    val bot = new GBot(
      randomTimeJitter,
      BotConfig(
        List(Wish.BuildSupply(SuppliesBuilding.MetalMine, 1, PlanetId)),
        buildMtUpToCapacity = false,
        useWishlist = true,
        activityFaker = false
      )
    )
    val prevState = State.LoggedIn(
      List.empty,
      List(
        createPlanetState(
          createSuppliesPage(
            resources = Resources(60, 15, 0),
            suppliesBuildingLevels = SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 1))
          )
        )
      ),
      List.empty
    )
    val nextState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List.empty)
  }

  test("should schedule building metal factory now if there is enough resources - with a jump") {
    val bot = new GBot(
      randomTimeJitter,
      BotConfig(
        List(Wish.BuildSupply(SuppliesBuilding.MetalMine, 10, PlanetId)),
        buildMtUpToCapacity = false,
        useWishlist = true,
        activityFaker = false
      )
    )
    val prevState = State.LoggedIn(List.empty, List(createPlanetState(createSuppliesPage(resources = Resources(60, 15, 0)))), List.empty)
    val nextState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.BuildSupply(SuppliesBuilding.MetalMine, 1, now, PlanetId)))
  }

  test("should not schedule building metal factory if there is something scheduled") {
    val bot = new GBot(
      randomTimeJitter,
      BotConfig(
        List(Wish.BuildSupply(SuppliesBuilding.MetalMine, 1, PlanetId)),
        buildMtUpToCapacity = false,
        useWishlist = true,
        activityFaker = false
      )
    )

    val prevState = State.LoggedIn(
      List(Task.BuildSupply(SuppliesBuilding.CrystalMine, 1, now, PlanetId)),
      List(createPlanetState(createSuppliesPage(resources = Resources(60, 15, 0)))),
      List.empty
    )
    val nextState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.BuildSupply(SuppliesBuilding.CrystalMine, 1, now, PlanetId)))
  }
  test("should schedule building metal factory in the future if there is not enough resources") {
    val bot = new GBot(
      randomTimeJitter,
      BotConfig(
        List(Wish.BuildSupply(SuppliesBuilding.MetalMine, 1, PlanetId)),
        buildMtUpToCapacity = false,
        useWishlist = true,
        activityFaker = false
      )
    )
    val prevState = State.LoggedIn(List.empty, List(createPlanetState(createSuppliesPage(production = Resources(10, 10, 10)))), List.empty)

    val nextState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.BuildSupply(SuppliesBuilding.MetalMine, 1, now.plusSeconds(6 * 3600), PlanetId)))
  }

  test("should schedule building metal factory in the future if there is not enough resources - with a jump") {
    val bot = new GBot(
      randomTimeJitter,
      BotConfig(
        List(Wish.BuildSupply(SuppliesBuilding.MetalMine, 10, PlanetId)),
        buildMtUpToCapacity = false,
        useWishlist = true,
        activityFaker = false
      )
    )
    val prevState = State.LoggedIn(List.empty, List(createPlanetState(createSuppliesPage(production = Resources(10, 10, 10)))), List.empty)

    val nextState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.BuildSupply(SuppliesBuilding.MetalMine, 1, now.plusSeconds(6 * 3600), PlanetId)))
  }

  test("should not build building if it is already built") {
    val bot = new GBot(
      randomTimeJitter,
      BotConfig(
        List(Wish.BuildSupply(SuppliesBuilding.MetalMine, 1, PlanetId)),
        buildMtUpToCapacity = false,
        useWishlist = true,
        activityFaker = false
      )
    )
    val prevState = State.LoggedIn(
      List.empty,
      List(
        createPlanetState(
          createSuppliesPage(
            production = Resources(10, 10, 10),
            suppliesBuildingLevels = SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 1))
          )
        )
      ),
      List.empty
    )

    val nextState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List.empty)
  }

  test("should schedule refresh after building finishes") {
    val bot = new GBot(
      randomTimeJitter,
      BotConfig(
        List(Wish.BuildSupply(SuppliesBuilding.MetalMine, 1, PlanetId)),
        buildMtUpToCapacity = false,
        useWishlist = true,
        activityFaker = false
      )
    )
    val prevState = State.LoggedIn(
      List.empty,
      List(
        createPlanetState(
          createSuppliesPage(production = Resources(10, 10, 10), currentBuildinProgress = Some(BuildingProgress(now.plusSeconds(1))))
        )
      ),
      List.empty
    )

    val nextState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.RefreshSupplyAndFacilityPage(now.plusSeconds(1), PlanetId)))
  }

  test("should build storage if there is not enough capacity") {
    val bot = new GBot(
      randomTimeJitter,
      BotConfig(
        List(Wish.BuildSupply(SuppliesBuilding.MetalMine, 11, PlanetId)),
        buildMtUpToCapacity = false,
        useWishlist = true,
        activityFaker = false
      )
    )
    val prevState = State.LoggedIn(
      List.empty,
      List(
        createPlanetState(
          createSuppliesPage(
            resources = Resources.Zero,
            production = Resources(100, 0, 0),
            capacity = Resources(1500, 0, 0),
            suppliesBuildingLevels = SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 10))
          )
        )
      ),
      List.empty
    )
    val nextState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.BuildSupply(SuppliesBuilding.MetalStorage, 1, now.plusSeconds(10 * 3600), PlanetId)))
  }

  test("should build power plant if there is not enough energy") {
    val bot = new GBot(
      randomTimeJitter,
      BotConfig(
        List(Wish.BuildSupply(SuppliesBuilding.MetalMine, 11, PlanetId)),
        buildMtUpToCapacity = false,
        useWishlist = true,
        activityFaker = false
      )
    )
    val prevState = State.LoggedIn(
      List.empty,
      List(
        createPlanetState(
          createSuppliesPage(resources = Resources(0, 0, 0, -100), production = Resources(75, 30, 0), capacity = Resources(2000, 2000, 0))
        )
      ),
      List.empty
    )

    val nextState = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.BuildSupply(SuppliesBuilding.SolarPlant, 1, now.plusSeconds(1 * 3600), PlanetId)))
  }
  //TODO ?? suppliesBuildings map -> case class
  //TODO resources @newtype metal, crystal, deuter
}
