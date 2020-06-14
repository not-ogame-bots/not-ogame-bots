package not.ogame.bots.selenium

import not.ogame.bots.{Credentials, FleetId, PlanetId}

class OgameUrlProvider(credentials: Credentials) extends UrlProvider {
  override def universeListUrl: String = {
    "https://lobby.ogame.gameforge.com/pl_PL/accounts"
  }

  def suppliesPageUrl(planetId: String): String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=supplies&cp=$planetId"
  }

  def facilitiesPageUrl(planetId: String): String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=facilities&cp=$planetId"
  }

  def getShipyardUrl(planetId: String): String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=shipyard&cp=$planetId"
  }

  def getTechnologyUrl(planetId: String): String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=research&cp=$planetId"
  }

  def readAllFleetsUrl: String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=componentOnly&component=eventList&ajax=1"
  }

  def readMyFleetsUrl: String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=movement"
  }

  def getFleetDispatchUrl(planetId: PlanetId): String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=fleetdispatch&cp=$planetId"
  }

  override def returnFleetUrl(fleetId: FleetId): String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=movement&return=${fleetId.filter(_.isDigit)}"
  }

  override def planetsUrl: String = {
    s"https://${credentials.universeId}.ogame.gameforge.com/game/index.php?page=ingame&component=overview"
  }
}
