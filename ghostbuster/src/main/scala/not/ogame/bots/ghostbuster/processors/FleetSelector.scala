package not.ogame.bots.ghostbuster.processors
import not.ogame.bots.{FleetPageData, ShipType}

class FleetSelector(filters: Map[ShipType, Int => Int] = Map()) extends (FleetPageData => Map[ShipType, Int]) {
  override def apply(fleetPageData: FleetPageData): Map[ShipType, Int] = {
    ShipType.values.map(shipType => shipType -> computeShipToSend(shipType, fleetPageData.ships(shipType))).toMap.filter {
      case (_, count) => count > 0
    }
  }

  private def computeShipToSend(shipType: ShipType, countOnPlanet: Int): Int = {
    if (filters.contains(shipType)) {
      filters(shipType)(countOnPlanet)
    } else {
      countOnPlanet
    }
  }
}
