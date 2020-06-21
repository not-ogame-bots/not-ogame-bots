package not.ogame.bots.ghostbuster.processors

import cats.Monad
import not.ogame.bots.{OgameDriver, PlayerPlanet, Resources}
import cats.implicits._

class ResourceSelector[T[_]: Monad](
    metalSelector: Int => Int = Selector.all,
    crystalSelector: Int => Int = Selector.all,
    deuteriumSelector: Int => Int = Selector.all
) {
  def selectResources(ogameDriver: OgameDriver[T], playerPlanet: PlayerPlanet): T[Resources] = {
    ogameDriver
      .readSuppliesPage(playerPlanet.id)
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
