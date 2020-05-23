package not.ogame.bots.ghostbuster

import java.time.temporal.ChronoUnit
import java.time.{Clock, ZoneId}

import not.ogame.bots.Resources
import not.ogame.bots.ghostbuster.processors.InactivityProcessor

class InactivityProcessorSpec extends munit.FunSuite {
  private implicit val clock: Clock = Clock.fixed(now, ZoneId.systemDefault())
  private val processor = new InactivityProcessor(
    BotConfig(List.empty, buildMtUpToCapacity = false, useWishlist = false, activityFaker = true)
  )

  test("should schedule next action in 14 minutes") {
    val prevState = State.LoggedIn(List.empty, List(createPlanetState(createSuppliesPage(resources = Resources(60, 15, 0)))), List.empty)

    val timestamp = clock.instant()
    val state = processor.apply(prevState)

    assertEquals(state.scheduledTasks, List(Task.DumpActivity(timestamp.plus(14, ChronoUnit.MINUTES), List(PlanetId))))
  }

  test("should not schedule dump action if it is already scheduled") {
    val prevState = State.LoggedIn(
      List(Task.DumpActivity(clock.instant().plus(14, ChronoUnit.MINUTES), List(PlanetId))),
      List(createPlanetState(createSuppliesPage(resources = Resources(60, 15, 0)))),
      List.empty
    )

    val state = processor.apply(prevState)

    assertEquals(state.scheduledTasks.size, 1)
  }
}
