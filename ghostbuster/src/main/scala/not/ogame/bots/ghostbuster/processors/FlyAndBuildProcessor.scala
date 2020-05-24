package not.ogame.bots.ghostbuster.processors

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit

import cats.effect.concurrent.Ref
import cats.implicits._
import eu.timepit.refined.numeric.Positive
import monix.eval.Task
import not.ogame.bots._
import not.ogame.bots.facts.SuppliesBuildingCosts
import not.ogame.bots.ghostbuster.{BotConfig, Wish}
import not.ogame.bots.selenium.refineVUnsafe

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

class FlyAndBuildProcessor(taskExecutor: TaskExecutor, botConfig: BotConfig, clock: Clock) {
  println(s"wishlist: ${pprint.apply(botConfig)}")
  private val planetSendingCount = Ref.unsafe[Task, Int](0)

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
    for {
      counter <- planetSendingCount.get
      targetPlanet = otherPlanets(counter % otherPlanets.size)
      _ <- buildAndContinue(currentPlanet, clock.instant())
      _ <- sendFleet(from = currentPlanet, to = targetPlanet)
      _ <- buildAndSend(currentPlanet = targetPlanet, planets)
    } yield ()
  }

  private def sendFleet(from: PlayerPlanet, to: PlayerPlanet): Task[Unit] = { //TODO if couldn't take all resources then build mt
    for {
      _ <- planetSendingCount.update(_ + 1)
      arrivalTime <- taskExecutor
        .sendFleet(
          SendFleetRequest(
            from.id,
            SendFleetRequestShips.Ships(botConfig.fs.ships.map(s => s.shipType -> s.amount).toMap),
            to.coordinates,
            FleetMissionType.Deployment,
            FleetResources.Max
          )
        )
      _ <- taskExecutor.waitTo(arrivalTime)
    } yield ()
  }

  private def buildAndContinue(planet: PlayerPlanet, startedBuildingAt: Instant): Task[Unit] = { //TODO it should be inside smart builder not outside
    buildNextThingFromWishList(planet).flatMap {
      case Some(elapsedTime)
          if timeDiff(elapsedTime, clock.instant()) < (10 minutes) && timeDiff(startedBuildingAt, clock.instant()) < (20 minutes) =>
        taskExecutor.waitTo(elapsedTime) >> buildAndContinue(planet, startedBuildingAt)
      case _ => Task.unit
    }
  }

  private def timeDiff(first: Instant, seconds: Instant): FiniteDuration = {
    FiniteDuration(first.toEpochMilli - seconds.toEpochMilli, TimeUnit.MILLISECONDS)
  }

  private def buildNextThingFromWishList(planet: PlayerPlanet): Task[Option[Instant]] = {
    taskExecutor.readSupplyPage(planet).flatMap { suppliesPageData =>
      if (!suppliesPageData.buildingInProgress) {
        botConfig.wishlist
          .collectFirst {
            case w: Wish.BuildSupply if suppliesPageData.getLevel(w.suppliesBuilding) < w.level.value && w.planetId == planet.id =>
              if (!suppliesPageData.buildingInProgress) {
                buildSupplyBuildingOrNothing(w.suppliesBuilding, suppliesPageData, planet)
              } else {
                suppliesPageData.currentBuildingProgress.map(_.finishTimestamp).pure[Task]
              }
            case w: Wish.SmartSupplyBuilder if isSmartBuilderApplicable(planet, suppliesPageData, w) =>
              println("Smart builder applicable")
              if (!suppliesPageData.buildingInProgress) {
                smartBuilder(planet, suppliesPageData)
              } else {
                suppliesPageData.currentBuildingProgress.map(_.finishTimestamp).pure[Task]
              }
          }
          .sequence
          .map(_.flatten)
      } else {
        Option.empty[Instant].pure[Task]
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
        suppliesPageData.getLevel(SuppliesBuilding.CrystalMine) > 2
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
  ): Task[Option[Instant]] = {
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
      taskExecutor.buildSupplyBuilding(suppliesBuilding, level, planet).map(Some(_))
    } else {
      Task.unit.map(_ => None)
    }
  }

  private def nextLevel(suppliesPage: SuppliesPageData, building: SuppliesBuilding) = {
    refineVUnsafe[Positive, Int](suppliesPage.suppliesLevels.values(building).value + 1)
  }
}
