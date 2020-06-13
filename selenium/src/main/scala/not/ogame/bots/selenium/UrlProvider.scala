package not.ogame.bots.selenium

import not.ogame.bots.{FleetId, PlanetId}

trait UrlProvider {
  def universeListUrl: String

  def suppliesPageUrl(planetId: String): String

  def facilitiesPageUrl(planetId: String): String

  def getShipyardUrl(planetId: String): String

  def getTechnologyUrl(planetId: String): String

  def readMyFleetsUrl: String

  def readAllFleetsUrl: String

  def getFleetDispatchUrl(planetId: PlanetId): String

  def returnFleetUrl(fleetId: FleetId): String

  def planetsUrl: String
}
