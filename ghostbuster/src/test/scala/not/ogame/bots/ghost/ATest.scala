package not.ogame.bots.ghost

import java.time.{Clock, Instant, LocalDateTime, ZoneOffset, ZonedDateTime}

class ATest extends munit.FunSuite {
  val clock1 = Clock.system(ZoneOffset.ofHours(3))

  test("date") {
    val ldt = LocalDateTime.now(clock1)
    println(s"bare ldt ${LocalDateTime.now()}")
    println(s"clock ldt $ldt")
    println(s"bare instant ${Instant.now()}")
    println(s"clock instant ${clock1.instant()}")
    println(s"ldt to instant using clock ${ldt.toInstant(ZoneOffset.of(clock1.getZone.getId))}")
    println(s"ldt to insstant w/o clock ${ldt.toInstant(ZoneOffset.ofHours(0))}")

    val zdt = ZonedDateTime.now(clock1)
    println(s"bare ldt ${ZonedDateTime.now()}")
    println(s"clock ldt $zdt")
  }
}
