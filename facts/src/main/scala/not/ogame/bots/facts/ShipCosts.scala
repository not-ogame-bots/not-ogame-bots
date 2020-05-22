package not.ogame.bots.facts

import not.ogame.bots.{Resources, ShipType}

object ShipCosts {
  def shipCost(shipType: ShipType): Resources = {
    shipType match {
      case ShipType.LIGHT_FIGHTER    => ???
      case ShipType.HEAVY_FIGHTER    => ???
      case ShipType.CRUISER          => ???
      case ShipType.BATTLESHIP       => ???
      case ShipType.INTERCEPTOR      => ???
      case ShipType.DESTROYER        => ???
      case ShipType.EXPLORER         => ???
      case ShipType.SMALL_CARGO_SHIP => Resources(2000, 2000, 0)
      case ShipType.LARGE_CARGO_SHIP => ???
      case ShipType.RECYCLER         => ???
      case ShipType.ESPIONAGE_PROBE  => ???
      case ShipType.BOMBER           => ???
      case ShipType.REAPER           => ???
    }
  }
}
