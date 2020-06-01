package not.ogame.bots

trait UrlProvider {
  def universeListUrl: String

  def getFleetDispatchUrl(planetId: String): String

  def suppliesPageUrl(planetId: String): String

  def facilitiesPageUrl(planetId: String): String

  def getShipyardUrl(planetId: String): String

  def getTechnologyUrl(planetId: String): String

  def readMyFleetsUrl: String
  
  def readAllFleetsUrl: String
}
