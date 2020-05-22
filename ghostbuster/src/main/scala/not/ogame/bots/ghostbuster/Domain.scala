package not.ogame.bots.ghostbuster

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import not.ogame.bots.{SuppliesBuilding, SuppliesPageData}

sealed trait Task {
  def executeAfter: Instant
}
object Task {
  case class BuildSupply(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, executeAfter: Instant) extends Task
  case class Refresh(executeAfter: Instant) extends Task

  def build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, executeAfter: Instant): Task = {
    BuildSupply(suppliesBuilding, level, executeAfter)
  }

  def refresh(executeAfter: Instant): Task = Task.Refresh(executeAfter)
}

sealed trait Wish
object Wish {
  case class Build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive) extends Wish

  def build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive): Wish = Build(suppliesBuilding, level)
}

sealed trait PlanetState {
  def scheduledTasks: List[Task]
}
object PlanetState {
  case class LoggedOut(scheduledTasks: List[Task]) extends PlanetState
  case class LoggedIn(suppliesPage: SuppliesPageData, scheduledTasks: List[Task]) extends PlanetState

  def loggedIn(suppliesPage: SuppliesPageData, scheduledTasks: List[Task]): PlanetState = {
    PlanetState.LoggedIn(suppliesPage, scheduledTasks)
  }
}

case class BotConfig(wishlist: List[Wish])
