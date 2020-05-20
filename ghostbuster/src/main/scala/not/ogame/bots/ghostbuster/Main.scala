package not.ogame.bots.ghostbuster

import java.time.{Instant, LocalDateTime}

import cats.effect.{ExitCode, IO, IOApp}
import not.ogame.bots.{Credentials, SuppliesBuilding, SuppliesPageData}
import not.ogame.bots.SuppliesBuilding.MetalMine
import not.ogame.bots.selenium.SeleniumOgameDriverCreator
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val credentials = ConfigSource.file(s"${System.getenv("HOME")}/.not-ogame-bots/credentials.conf").loadOrThrow[Credentials]
    new SeleniumOgameDriverCreator()
      .create(credentials)
      .use { ogame =>
        ogame.login() >>
          ogame.readSuppliesPage("33794124").map(println(_)) >>
          ogame.buildSuppliesBuilding("33794124", MetalMine) >>
          ogame.readSuppliesPage("33794124").map(println(_)) >>
          IO.never
      }
      .as(ExitCode.Success)
  }
}

sealed trait Task {
  def executeAfter: Instant
}
object Task {
  case class Build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, executeAfter: Instant) extends Task
  case class Login(executeAfter: Instant) extends Task

  def build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, executeAfter: Instant): Task = {
    Build(suppliesBuilding, level, executeAfter)
  }

  def login(executeAfter: Instant): Task = Task.Login(executeAfter)
}

sealed trait Wish
object Wish {
  case class Build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive) extends Wish

  def build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive): Wish = Build(suppliesBuilding, level)
}

sealed trait State {
  def scheduledTasks: List[Task]
  def wishList: List[Wish]
}
object State {
  case class LoggedOut(scheduledTasks: List[Task], wishList: List[Wish]) extends State
  case class LoggedIn(suppliesPage: SuppliesPageData, wishList: List[Wish], scheduledTasks: List[Task]) extends State
}
// lista zadan ulozonych w czasie

// metoda check ktora
// a) tworzy rzeczy na liscie
// b) sprawdza liste i wykonuje z niej zadania
// wish list - podawane przez usera
