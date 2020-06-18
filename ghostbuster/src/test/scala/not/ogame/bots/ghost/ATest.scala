package not.ogame.bots.ghost

import java.time.{Clock, Instant, ZoneOffset, ZonedDateTime}

import not.ogame.bots.RealLocalClock

class ATest extends munit.FunSuite {
  val clock1 = Clock.system(ZoneOffset.ofHours(3))

  test("date") {
    val ldt = ZonedDateTime.now(clock1)
    val ldtNow = ZonedDateTime.now()
    println(s"bare ldt $ldtNow")
    println(s"clock ldt $ldt")
    println(s"bare instant ${Instant.now()}")
    println(s"clock instant ${clock1.instant()}")
    println(s"ldt to instant using clock ${ldt.toInstant}")
    println(s"ldt to insstant w/o clock ${ldtNow.toInstant}")

    val zdt = ZonedDateTime.now(clock1)
    println(s"bare ldt ${ZonedDateTime.now()}")
    println(s"clock ldt $zdt")
  }

  test("two dates should be the same according to seconds") {
    val realDateTime = new RealLocalClock()
    val time1 = realDateTime.now()
    val time2 = realDateTime.now()
    assertEquals(time1, time2)
  }
}
