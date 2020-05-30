package not.ogame.bots.ordon

import java.time.ZonedDateTime

import cats.Monad
import cats.implicits._
import not.ogame.bots.SuppliesBuilding._
import not.ogame.bots._

import scala.collection.mutable.ListBuffer
import scala.util.Random

class BuildOgameAction[T[_]: Monad](playerPlanet: PlayerPlanet)(implicit clock: LocalClock) extends SimpleOgameAction[T] {
  override def processSimple(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      page <- ogame.readSuppliesPage(playerPlanet.id)
      resumeOn <- if (page.currentBuildingProgress.isDefined) page.currentBuildingProgress.get.finishTimestamp.pure[T] else build(ogame)
    } yield resumeOn

  private def build(ogame: OgameDriver[T]): T[ZonedDateTime] =
    for {
      page <- ogame.readSuppliesPage(playerPlanet.id)
      toBuild = choose(page)
      resumeOn <- if (toBuild.isDefined) ogame.buildSuppliesBuilding(playerPlanet.id, toBuild.get).map(_ => clock.now())
      else clock.now().plusDays(1).pure[T]
    } yield resumeOn

  private def choose(page: SuppliesPageData): Option[SuppliesBuilding] = {
    val list = new ListBuffer[SuppliesBuilding]
    if (page.getLevel(MetalMine).value < 26) list.addOne(MetalMine)
    if (page.getLevel(CrystalMine).value < 25) list.addOne(CrystalMine)
    if (page.getLevel(DeuteriumSynthesizer).value < 22) list.addOne(DeuteriumSynthesizer)
    if (page.getLevel(SolarPlant).value < 27) list.addOne(SolarPlant)
    if (page.getLevel(MetalStorage).value < 9) list.addOne(MetalStorage)
    if (page.getLevel(CrystalStorage).value < 8) list.addOne(CrystalStorage)
    if (page.getLevel(DeuteriumStorage).value < 6) list.addOne(DeuteriumStorage)
    Random.shuffle(list).headOption
  }
}
