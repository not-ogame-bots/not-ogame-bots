package not.ogame.bots.ghostbuster

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots.{SuppliesBuilding, SuppliesPageData}

sealed trait Task {
  def executeAfter: Instant
}
object Task {
  case class Build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, executeAfter: Instant) extends Task
  case class Login(executeAfter: Instant) extends Task
  case class Refresh(executeAfter: Instant) extends Task

  def build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, executeAfter: Instant): Task = {
    Build(suppliesBuilding, level, executeAfter)
  }

  def login(executeAfter: Instant): Task = Task.Login(executeAfter)

  def refresh(executeAfter: Instant): Task = Task.Refresh(executeAfter)
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

  def loggedIn(suppliesPage: SuppliesPageData, wishList: List[Wish], scheduledTasks: List[Task]): State = {
    State.LoggedIn(suppliesPage, wishList, scheduledTasks)
  }
}

case class BotConfig(wishlist: List[Wish])
