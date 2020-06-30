package not.ogame.bots.ordon.action

import not.ogame.bots.facts.TechnologyCosts
import not.ogame.bots.ordon.core._
import not.ogame.bots.{PlayerPlanet, Technology}

class ResearchOrdonAction(planet: PlayerPlanet, researchList: List[(Technology, Int)]) extends BaseOrdonAction {
  override def shouldHandleEvent(event: OrdonEvent): Boolean = {
    event match {
      case ChangeOnPlanet(_, changedPlanet) => changedPlanet == planet
      case _                                => false
    }
  }

  override def doProcess(event: OrdonEvent, ogame: OrdonOgameDriver, eventRegistry: EventRegistry): List[OrdonAction] = {
    if (researchList.isEmpty) {
      List()
    } else {
      val pageData = ogame.readTechnologyPage(planet.id)
      val cost = TechnologyCosts.technologyCost(researchList.head._1, researchList.head._2)
      if (pageData.currentResources.gtEqTo(cost) && pageData.currentResearchProgress.isEmpty) {
        ogame.startResearch(planet.id, researchList.head._1)
        List(new ResearchOrdonAction(planet, researchList.tail))
      } else {
        List(this)
      }
    }
  }

  override def toString: String = s"Research $researchList"
}
