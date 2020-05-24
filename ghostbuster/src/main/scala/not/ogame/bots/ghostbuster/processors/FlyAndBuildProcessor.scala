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
          println(s"Found our fleet in the air: ${pprint.apply(fleet)}")
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
      .map(_.maxBy(_.fleet.count { case (_, c) => c > 0 }))

    planetWithBiggestFleet.flatMap { planet =>
      println(s"Planet with biggest fleet ${pprint.apply(planet)}")
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
            case w: Wish.BuildSupply if suppliesPageData.getLevel(w.suppliesBuilding) < w.level.value && w.planetId == planet.id =>
              buildSupplyBuildingOrNothing(w.suppliesBuilding, suppliesPageData, planet)
            case w: Wish.SmartSupplyBuilder if isSmartBuilderApplicable(planet, suppliesPageData, w) =>
              smartBuilder(planet, suppliesPageData)
          }
          .sequence
          .void
      } else {
        Task.unit
      }
    }
  }

  private def smartBuilder(planet: PlayerPlanet, suppliesPageData: SuppliesPageData) = {
    if (suppliesPageData.currentResources.energy < 0) {
      buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.SolarPlant)
    } else {
      val shouldBuildDeuter = suppliesPageData.getLevel(SuppliesBuilding.MetalMine) -
        suppliesPageData.getLevel(SuppliesBuilding.DeuteriumSynthesizer) > 3
      val shouldBuildCrystal = suppliesPageData.getLevel(SuppliesBuilding.MetalMine) -
        suppliesPageData.getLevel(SuppliesBuilding.CrystalStorage) > 1
      if (shouldBuildDeuter) {
        buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.DeuteriumSynthesizer)
      } else if (shouldBuildCrystal) {
        buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.CrystalMine)
      } else {
        buildBuildingOrStorage(planet, suppliesPageData, SuppliesBuilding.MetalMine)
      }
    }
  }

  private def buildBuildingOrStorage(planet: PlayerPlanet, suppliesPageData: SuppliesPageData, building: SuppliesBuilding) = {
    val level = nextLevel(suppliesPageData, building)
    val requiredResources = SuppliesBuildingCosts.buildingCost(building, level)
    if (suppliesPageData.currentCapacity.gtEqTo(requiredResources)) {
      buildSupplyBuildingOrNothing(building, suppliesPageData, planet)
    } else {
      buildStorage(suppliesPageData, requiredResources, planet)
    }
  }

  private def buildStorage(
      suppliesPage: SuppliesPageData,
      requiredResources: Resources,
      planet: PlayerPlanet
  ): Task[Unit] = {
    requiredResources.difference(suppliesPage.currentCapacity) match {
      case Resources(m, _, _, _) if m > 0 =>
        buildSupplyBuildingOrNothing(SuppliesBuilding.MetalStorage, suppliesPage, planet)
      case Resources(_, c, _, _) if c > 0 =>
        buildSupplyBuildingOrNothing(SuppliesBuilding.CrystalStorage, suppliesPage, planet)
      case Resources(_, _, d, _) if d > 0 =>
        buildSupplyBuildingOrNothing(SuppliesBuilding.DeuteriumStorage, suppliesPage, planet)
    }
  }

  private def isSmartBuilderApplicable(planet: PlayerPlanet, suppliesPageData: SuppliesPageData, w: Wish.SmartSupplyBuilder) = {
    val correctPlanet = w.planetId == planet.id
    val metalMineUnderLevel = w.metalLevel.value > suppliesPageData.getLevel(SuppliesBuilding.MetalMine)
    val crystalMineUnderLevel = w.crystalLevel.value > suppliesPageData.getLevel(SuppliesBuilding.CrystalMine)
    val deuterMineUnderLevel = w.deuterLevel.value > suppliesPageData.getLevel(SuppliesBuilding.DeuteriumSynthesizer)
    correctPlanet && (metalMineUnderLevel || crystalMineUnderLevel || deuterMineUnderLevel)
  }

  private def buildSupplyBuildingOrNothing(suppliesBuilding: SuppliesBuilding, suppliesPageData: SuppliesPageData, planet: PlayerPlanet) = {
    val level = nextLevel(suppliesPageData, suppliesBuilding)
    val requiredResources =
      SuppliesBuildingCosts.buildingCost(suppliesBuilding, level)
    if (suppliesPageData.currentResources.gtEqTo(requiredResources)) {
      taskExecutor.buildSupplyBuilding(suppliesBuilding, level, planet).void
    } else {
      Task.unit
    }
  }

  private def nextLevel(suppliesPage: SuppliesPageData, building: SuppliesBuilding) = {
    refineVUnsafe[Positive, Int](suppliesPage.suppliesLevels.values(building).value + 1)
  }
}
