//package not.ogame.bots.ordon.core
//
//import cats.Id
//import not.ogame.bots.CoordinatesType.Moon
//import not.ogame.bots.ShipType.{Destroyer, EspionageProbe, Explorer, LargeCargoShip}
//import not.ogame.bots.ordon.OrdonQuasarConfig
//import not.ogame.bots.ordon.action.ExpeditionOrdonAction
//import cats.effect.{IO, Sync}
//import cats.implicits._
//import not.ogame.bots.selenium.{OgameUrlProvider, SeleniumOgameDriver, WebDriverResource}
//import not.ogame.bots.{Coordinates, OgameDriver, PlanetId, PlayerPlanet}
//
//object Boot {
//  def main(args: Array[String]): Unit = {
//    WebDriverResource.firefox[Id]()
//    Sync[Id].
//    val credentials = OrdonQuasarConfig.getCredentials
//    val expeditionAction = new ExpeditionOrdonAction(moon, Map(Destroyer -> 1, EspionageProbe -> 1, LargeCargoShip -> 400, ExStop using Start actionplorer -> 20))
//    new SeleniumOgameDriver[Id](credentials, new OgameUrlProvider(credentials))() {}
//    while (true) {}
//  }
//
//  private val moon = PlayerPlanet(PlanetId.apply("33632870"), Coordinates(1, 155, 10, Moon))
//}
