package not.ogame.bots.ghostbuster.processors

import cats.Monad
import monix.eval.Task
import not.ogame.bots.{PlayerPlanet, Resources}

class ResourceSelector[T[_]: Monad](
    metalSelector: Int => Int = Selector.all,
    crystalSelector: Int => Int = Selector.all,
    deuteriumSelector: Int => Int = Selector.all
) {
  def selectResources(taskExecutor: TaskExecutor, playerPlanet: PlayerPlanet): Task[Resources] = {
    taskExecutor
      .readSupplyPage(playerPlanet)
      .map(_.currentResources)
      .map(
        it =>
          Resources(
            metal = metalSelector(it.metal),
            crystal = crystalSelector(it.crystal),
            deuterium = deuteriumSelector(it.deuterium)
          )
      )
  }
}

object Selector {
  def skip: Int => Int = _ => 0

  def all: Int => Int = countOnPlanet => countOnPlanet

  def decreaseBy(value: Int): Int => Int = countOnPlanet => Math.max(countOnPlanet - value, 0)
}
