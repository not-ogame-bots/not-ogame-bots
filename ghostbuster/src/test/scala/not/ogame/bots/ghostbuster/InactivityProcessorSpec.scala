package not.ogame.bots.ghostbuster

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}

import not.ogame.bots.{Resources, SuppliesBuildingLevels, SuppliesPageData}
import not.ogame.bots.ghostbuster.processors.InactivityProcessor

class InactivityProcessorSpec extends munit.FunSuite {
  private implicit val clock: Clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  private val processor = new InactivityProcessor()

  test("should schedule next action in 14 minutes") {
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        clock.instant(),
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

    val timestamp = clock.instant()
    val state = processor.apply(prevState)

    assertEquals(state.scheduledTasks, List(Task.DumpActivity(timestamp.plus(14, ChronoUnit.MINUTES))))
  }

  test("should not schedule dump action if it is already scheduled") {
    val prevState = PlanetState.LoggedIn(
      SuppliesPageData(
        clock.instant(),
        Resources(0, 0, 0),
        Resources(1, 1, 1),
        bigCapacity,
        SuppliesBuildingLevels(createStartingBuildings),
        Option.empty,
        Option.empty
      ),
      List(Task.DumpActivity(clock.instant().plus(14, ChronoUnit.MINUTES))),
      createFacilityBuildings,
      Map.empty
    )

    val state = processor.apply(prevState)

    assertEquals(state.scheduledTasks.size, 1)
  }
}
