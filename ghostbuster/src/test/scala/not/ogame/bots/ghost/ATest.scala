package not.ogame.bots.ghost

import java.time.{Clock, Instant, ZoneOffset, ZonedDateTime}

import io.circe.Printer
import not.ogame.bots.{LocalClock, RealLocalClock, SimplifiedDataTime}

class ATest extends munit.FunSuite {
  implicit val clock1 = Clock.system(ZoneOffset.ofHours(3))
  implicit val clock2: LocalClock = new RealLocalClock()

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

  test("ordring of sdt") {
    import SimplifiedDataTime._
    val ldt1 = ZonedDateTime.now(clock1)
    val ldt2 = ZonedDateTime.now(clock1)
    println(ldt1)
    println(ldt2.withYear(2021))
    println(List(ldt2, ldt2.withYear(2021)).map(SimplifiedDataTime.from(_)).min)
  }
}
