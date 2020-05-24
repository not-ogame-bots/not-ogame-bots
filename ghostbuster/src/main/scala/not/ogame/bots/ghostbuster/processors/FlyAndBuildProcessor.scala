package not.ogame.bots.ghostbuster.processors

import cats.implicits._
import eu.timepit.refined.numeric.Positive
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.facts.SuppliesBuildingCosts
import not.ogame.bots.ghostbuster.Wish
import not.ogame.bots.selenium.refineVUnsafe

class FlyAndBuildProcessor(taskExecutor: TaskExecutor, wishList: List[Wish]) {
  println(s"wishlist: ${pprint.apply(wishList)}")
  private var planetSendingCount = 0

  def run(): Task[Unit] = {
    for {
      planets <- taskExecutor.readPlanets()
      fleets <- taskExecutor.readAllFleets()
      _ <- fleets.find(
        f =>
          f.fleetAttitude == FleetAttitude.Friendly && f.fleetMissionType == FleetMissionType.Deployment && planets
            .exists(p => p.coordinates == f.to) && planets.exists(p => p.coordinates == f.from)
      ) match {
        case Some(fleet) =>
          val toPlanet = planets.find(p => fleet.to == p.coordinates).get
          taskExecutor.waitTo(fleet.arrivalTime) >> buildAndSend(toPlanet, planets)
        case None => lookAndSend(planets)
      }
    } yield ()
  }

  private def lookAndSend(planets: List[PlayerPlanet]): Task[Unit] = {
    val planetWithBiggestFleet = planets
      .map { planet =>
        taskExecutor.getFleetOnPlanet(planet)
      }
      .sequence
      .map(_.maxBy(_.fleet.size))

    planetWithBiggestFleet.flatMap { planet =>
      buildAndSend(planet.playerPlanet, planets)
    }
  }

  private def buildAndSend(currentPlanet: PlayerPlanet, planets: List[PlayerPlanet]): Task[Unit] = {
    val otherPlanets = planets.filterNot(p => p.id == currentPlanet.id)
    val targetPlanet = otherPlanets(planetSendingCount % otherPlanets.size)
    for {
      _ <- buildNextThingFromWishList(currentPlanet)
      _ <- sendFleet(from = currentPlanet, to = targetPlanet)
      _ <- buildAndSend(currentPlanet = targetPlanet, planets)
    } yield ()
  }

  private def sendFleet(from: PlayerPlanet, to: PlayerPlanet): Task[Unit] = {
    planetSendingCount = planetSendingCount + 1
    taskExecutor
      .sendFleet(
        SendFleetRequest(
          from.id,
          SendFleetRequestShips.AllShips,
          to.coordinates,
          FleetMissionType.Deployment,
          FleetResources.Max
        )
      )
      .flatMap(arrivalTime => taskExecutor.waitTo(arrivalTime))
  }

  private def buildNextThingFromWishList(planet: PlayerPlanet): Task[Unit] = {
    taskExecutor.readSupplyPage(planet).flatMap { suppliesPageData =>
      if (suppliesPageData.isIdle) {
        wishList
          .collectFirst {
            case w: Wish.BuildSupply
                if suppliesPageData.suppliesLevels.values(w.suppliesBuilding).value < w.level.value && w.planetId == planet.id =>
              buildSupplyBuilding(w, suppliesPageData, planet)
          }
          .sequence
          .void
      } else {
        Task.unit
      }
    }
  }

  private def buildSupplyBuilding(buildWish: Wish.BuildSupply, suppliesPageData: SuppliesPageData, planet: PlayerPlanet) = {
    val level = nextLevel(suppliesPageData, buildWish.suppliesBuilding)
    val requiredResources =
      SuppliesBuildingCosts.buildingCost(buildWish.suppliesBuilding, level)
    if (suppliesPageData.currentResources.gtEqTo(requiredResources)) { //TODO can be simplified
      taskExecutor.buildSupplyBuilding(buildWish.suppliesBuilding, level, planet).void
    } else {
      Task.unit
    }
  }

  private def nextLevel(suppliesPage: SuppliesPageData, building: SuppliesBuilding) = {
    refineVUnsafe[Positive, Int](suppliesPage.suppliesLevels.values(building).value + 1)
  }
}
