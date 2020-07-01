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
      val uncompleted = researchList.filter(t => pageData.getIntLevel(t._1) < t._2)
      if (uncompleted.isEmpty) {
        List()
      } else {
        val nextResearch = uncompleted.head
        val cost = TechnologyCosts.technologyCost(nextResearch._1, nextResearch._2)
        if (pageData.currentResources.gtEqTo(cost) && pageData.currentResearchProgress.isEmpty) {
          ogame.startResearch(planet.id, nextResearch._1)
          List(new ResearchOrdonAction(planet, uncompleted.tail))
        } else {
          List(new ResearchOrdonAction(planet, uncompleted))
        }
      }
    }
  }

  override def toString: String = s"Research $researchList"
}
