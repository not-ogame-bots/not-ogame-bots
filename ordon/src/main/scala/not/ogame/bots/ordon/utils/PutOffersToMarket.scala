package not.ogame.bots.ordon.utils

import cats.Monad
import cats.implicits._
import not.ogame.bots.{MyOffer, OgameDriver, PlayerPlanet}

class PutOffersToMarket {
  def putOffersToMarket[T[_]: Monad](ogame: OgameDriver[T], planet: PlayerPlanet, expectedOffers: List[MyOffer]): T[Unit] =
    for {
      myOffers <- ogame.readMyOffers()
      missingOffers = expectedOffers diff myOffers
      fleetPage <- ogame.readFleetPage(planet.id)
      freeTradeSlots = fleetPage.slots.maxTradeFleets - fleetPage.slots.currentTradeFleets
      offersToPlace = missingOffers.take(freeTradeSlots)
      _ = println(myOffers)
      _ = println(offersToPlace)
      _ <- oneAfterOther(offersToPlace.map(newOffer => ogame.createOffer(planet.id, newOffer)))
    } yield ()

  private def oneAfterOther[T[_]: Monad](actions: List[T[Unit]]): T[Unit] = {
    val unitT: T[Unit] = ().pure[T]
    actions.fold(unitT) { (a: T[Unit], b: T[Unit]) =>
      a.flatMap(_ => b)
    }
  }
}
