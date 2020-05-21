package not.ogame.bots.ghostbuster

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import eu.timepit.refined.auto._
import not.ogame.bots.{BuildingProgress, Resources, SuppliesBuilding, SuppliesBuildingLevels, SuppliesPageData}

class GBotSpec extends munit.FunSuite {
  private val now = Instant.now()
  private val unused: LocalDateTime = LocalDateTime.now()
  private implicit val clock: Clock = Clock.fixed(now, ZoneId.systemDefault())
  private val randomTimeJitter: RandomTimeJitter = () => 0
  private val bot = new GBot(randomTimeJitter)

  test("should do nothing if wishlist is empty") {
    val prevState = State.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(1, 1, 1),
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty
      ),
      List.empty,
      List.empty
    )
    val nextState: State = bot.nextStep(prevState)
    assertEquals(nextState, prevState)
  }

  test("should schedule building metal factory now if there is enough resources") {
    val prevState = State.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(60, 15, 0),
        Resources(0, 0, 0),
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty
      ),
      List(Wish.build(SuppliesBuilding.MetalMine, 1)),
      List.empty
    )
    val nextState: State = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.build(SuppliesBuilding.MetalMine, 1, now)))
  }

  test("should not schedule building metal factory if there is something scheduled") {
    val prevState = State.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(60, 15, 0),
        Resources(0, 0, 0),
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty
      ),
      List(Wish.build(SuppliesBuilding.MetalMine, 1)),
      List(Task.build(SuppliesBuilding.CrystalMine, 1, now))
    )
    val nextState: State = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.Build(SuppliesBuilding.CrystalMine, 1, now)))
  }
  test("should schedule building metal factory in the future if there is not enough resources") {
    val prevState = State.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(10, 10, 0),
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty
      ),
      List(Wish.build(SuppliesBuilding.MetalMine, 1)),
      List.empty
    )
    val nextState: State = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.build(SuppliesBuilding.MetalMine, 1, now.plusSeconds(6 * 3600))))
  }

  test("should not build building if it is already built") {
    val prevState = State.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(10, 10, 10),
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 1)),
        Option.empty
      ),
      List(Wish.build(SuppliesBuilding.MetalMine, 1)),
      List.empty
    )
    val nextState: State = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List.empty)
  }

  test("should schedule refresh if after building finishes") {
    val prevState = State.LoggedIn(
      SuppliesPageData(
        unused,
        Resources(0, 0, 0),
        Resources(10, 10, 10),
        SuppliesBuildingLevels(createStartingBuildings ++ Map(SuppliesBuilding.MetalMine -> 1)),
        Some(BuildingProgress(now.plusSeconds(1)))
      ),
      List(Wish.build(SuppliesBuilding.MetalMine, 1)),
      List.empty
    )
    val nextState: State = bot.nextStep(prevState)
    assertEquals(nextState.scheduledTasks, List(Task.refresh(now.plusSeconds(1))))
  }

  test("should schedule logging if it is logged out") {
    val state = State.LoggedOut(List.empty, List.empty)
    val nextState = bot.nextStep(state)
    assertEquals(nextState.scheduledTasks, List(Task.login(now)))
  }

  private def createStartingBuildings: Map[SuppliesBuilding, Int] = {
    SuppliesBuilding.values.map(_ -> 0).toMap
  }

  //TODO ?? suppliesBuildings map -> case class
  //TODO resources @newtype metal, crystal, deuter
}
