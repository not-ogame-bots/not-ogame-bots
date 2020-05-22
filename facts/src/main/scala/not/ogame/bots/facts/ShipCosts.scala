package not.ogame.bots.facts

import not.ogame.bots.{Resources, ShipType}

object ShipCosts {
  def shipCost(shipType: ShipType): Resources = {
    shipType match {
      case ShipType.LIGHT_FIGHTER    => Resources(3_000, 1_000, 0)
      case ShipType.HEAVY_FIGHTER    => Resources(6_000, 4_000, 0)
      case ShipType.CRUISER          => Resources(20_000, 7_000, 2_000)
      case ShipType.BATTLESHIP       => Resources(45_000, 15_000, 0)
      case ShipType.INTERCEPTOR      => Resources(40_000, 50_000, 15_000)
      case ShipType.BOMBER           => Resources(50_000, 25_000, 15_000)
      case ShipType.DESTROYER        => Resources(60_000, 50_000, 15_000)
      case ShipType.DEATH_STAR       => Resources(5_000_000, 4_000_000, 1_000_000)
      case ShipType.REAPER           => Resources(85_000, 55_000, 20_000)
      case ShipType.EXPLORER         => Resources(8_000, 15_000, 8_000)
      case ShipType.SMALL_CARGO_SHIP => Resources(2_000, 2_000, 0)
      case ShipType.LARGE_CARGO_SHIP => Resources(6_000, 6_000, 0)
      case ShipType.COLONY_SHIP      => Resources(10_000, 20_000, 10_000)
      case ShipType.RECYCLER         => Resources(10_000, 6_000, 2_000)
      case ShipType.ESPIONAGE_PROBE  => Resources(0, 1_000, 0)
    }
  }
}
