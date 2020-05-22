package not.ogame.bots.facts

import not.ogame.bots.{Resources, ShipType}

object ShipCosts {
  def shipCost(shipType: ShipType): Resources = {
    shipType match {
      case ShipType.LightFighter   => Resources(3_000, 1_000, 0)
      case ShipType.HeavyFighter   => Resources(6_000, 4_000, 0)
      case ShipType.Cruiser        => Resources(20_000, 7_000, 2_000)
      case ShipType.Battleship     => Resources(45_000, 15_000, 0)
      case ShipType.Interceptor    => Resources(40_000, 50_000, 15_000)
      case ShipType.Bomber         => Resources(50_000, 25_000, 15_000)
      case ShipType.Destroyer      => Resources(60_000, 50_000, 15_000)
      case ShipType.DeathStar      => Resources(5_000_000, 4_000_000, 1_000_000)
      case ShipType.Reaper         => Resources(85_000, 55_000, 20_000)
      case ShipType.Explorer       => Resources(8_000, 15_000, 8_000)
      case ShipType.SmallCargoShip => Resources(2_000, 2_000, 0)
      case ShipType.LargeCargoShip => Resources(6_000, 6_000, 0)
      case ShipType.ColonyShip     => Resources(10_000, 20_000, 10_000)
      case ShipType.Recycler       => Resources(10_000, 6_000, 2_000)
      case ShipType.EspionageProbe => Resources(0, 1_000, 0)
    }
  }
}
