package not.ogame.bots.ghostbuster

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.time.temporal.ChronoUnit

import not.ogame.bots.{Fleet, FleetAttitude, FleetMissionType}

class FlyAroundProcessorSpec extends munit.FunSuite {
  test("schedule sending fleet to another planet ") {
    val planetId2 = "2"
    println(LocalDateTime.now().toInstant(ZoneOffset.ofHours(2)))
    println(Instant.now())
    val state = State.LoggedIn(
      List.empty,
      List(createPlanetState(planetId = planetId1), createPlanetState(planetId = planetId2)),
      List(
        Fleet(
          LocalDateTime.ofInstant(now.plus(5, ChronoUnit.MINUTES), ZoneOffset.UTC),
          FleetAttitude.Friendly,
          FleetMissionType.Deployment,
          planet1Coords,
          planet2Coords,
          isReturning = false
        )
      )
    )
    println(state)
  }
}
