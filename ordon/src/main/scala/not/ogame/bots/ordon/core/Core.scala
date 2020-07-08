package not.ogame.bots.ordon.core

import java.time.{Duration, ZonedDateTime}

import not.ogame.bots.ordon.utils.SlackIntegration
import not.ogame.bots.{Coordinates, PlayerPlanet}

import scala.collection.mutable.ListBuffer

class Core(ogame: OrdonOgameDriver, initialActions: List[OrdonAction]) extends EventRegistry {
  private val slackIntegration: SlackIntegration = new SlackIntegration()
  val events: ListBuffer[OrdonEvent] = new ListBuffer[OrdonEvent]()

  override def registerEvent(event: OrdonEvent): Unit = {
    events.addOne(event)
  }

  def run(): Unit = {
    slackIntegration.postMessageToSlack("Starting core run")
    registerEvent(TimeBasedOrdonEvent(ZonedDateTime.now()))
    runInternal(initialActions)
  }

  @scala.annotation.tailrec
  private def runInternal(actions: List[OrdonAction]): Unit = {
    val firstEvent = popEarliestEvent()
    waitTo(firstEvent, actions)
    val newActions = actions.flatMap(action => {
      action.process(firstEvent, ogame, this)
    })
    // TODO: Fix Deploy and return - which got scheduled 24 ahead for some reason
    checkEventsWithinRange()
    runInternal(newActions)
  }

  private def popEarliestEvent(): OrdonEvent = {
    val firstEvent = events.min
    events.subtractOne(firstEvent)
    firstEvent
  }

  private def waitTo(firstEvent: OrdonEvent, actions: List[OrdonAction]): Unit = {
    val now = ZonedDateTime.now()
    val millis = Duration.between(now, firstEvent.triggerOn).toMillis
    if (millis > 0) {
      println()
      println(s"Time now is: $now")
      println(s"Waiting  to: ${firstEvent.triggerOn}")
      actions.foreach(action => println(action))
      Thread.sleep(millis)
    }
  }

  private def checkEventsWithinRange(): Unit = {
    val maxTriggerOn = events.max.triggerOn
    if (maxTriggerOn.isAfter(ZonedDateTime.now().plusHours(20))) {
      throw new RuntimeException(s"Got an event with suspicious triggerOn: $maxTriggerOn")
    }
  }
}

trait OrdonAction {
  def process(event: OrdonEvent, ogame: OrdonOgameDriver, eventRegistry: EventRegistry): List[OrdonAction]
}

abstract class BaseOrdonAction extends OrdonAction {
  final override def process(event: OrdonEvent, ogame: OrdonOgameDriver, eventRegistry: EventRegistry): List[OrdonAction] = {
    if (shouldHandleEvent(event)) {
      doProcess(event, ogame, eventRegistry)
    } else {
      List(this)
    }
  }

  def shouldHandleEvent(event: OrdonEvent): Boolean

  def doProcess(event: OrdonEvent, ogame: OrdonOgameDriver, eventRegistry: EventRegistry): List[OrdonAction]
}

abstract class EndlessOrdonAction extends BaseOrdonAction {
  final override def doProcess(event: OrdonEvent, ogame: OrdonOgameDriver, eventRegistry: EventRegistry): List[OrdonAction] = {
    eventRegistry.registerEvent(doProcessEndless(event, ogame, eventRegistry))
    List(this)
  }

  def doProcessEndless(event: OrdonEvent, ogame: OrdonOgameDriver, eventRegistry: EventRegistry): OrdonEvent
}

abstract class TimeBasedOrdonAction extends EndlessOrdonAction {
  protected var resumeOn: ZonedDateTime = null

  final override def shouldHandleEvent(event: OrdonEvent): Boolean = {
    resumeOn == null || !event.triggerOn.isBefore(resumeOn)
  }

  final override def doProcessEndless(event: OrdonEvent, ogame: OrdonOgameDriver, eventRegistry: EventRegistry): OrdonEvent = {
    resumeOn = processTimeBased(ogame, eventRegistry)
    TimeBasedOrdonEvent(resumeOn)
  }

  def processTimeBased(ogame: OrdonOgameDriver, eventRegistry: EventRegistry): ZonedDateTime
}

trait EventRegistry {
  def registerEvent(event: OrdonEvent): Unit
}

trait OrdonEvent extends Comparable[OrdonEvent] {
  val triggerOn: ZonedDateTime

  override def compareTo(other: OrdonEvent): Int =
    triggerOn.compareTo(other.triggerOn)
}

case class TimeBasedOrdonEvent(override val triggerOn: ZonedDateTime) extends OrdonEvent

case class ChangeOnPlanet(override val triggerOn: ZonedDateTime, planet: PlayerPlanet) extends OrdonEvent

case class ExpeditionFleetChanged(override val triggerOn: ZonedDateTime, to: Coordinates) extends OrdonEvent
