package not.ogame.bots.ordon.core

import java.time.{Duration, ZonedDateTime}

import scala.collection.mutable.ListBuffer

class Core(ordonOgameDriver: OrdonOgameDriver, initialActions: List[OrdonAction]) extends EventRegistry {
  val events: ListBuffer[OrdonEvent] = new ListBuffer[OrdonEvent]()

  override def registerEvent(event: OrdonEvent): Unit = {
    events.addOne(event)
  }

  def run(): Unit = {
    println("Starting core run")
    registerEvent(TimeBasedOrdonEvent(ZonedDateTime.now()))
    runInternal(initialActions)
  }

  @scala.annotation.tailrec
  private def runInternal(actions: List[OrdonAction]): Unit = {
    val firstEvent = popEarliestEvent()
    waitTo(firstEvent, actions)
    val newActions = actions.flatMap(action => {
      action.process(firstEvent, ordonOgameDriver, this)
    })
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
      println(s"Time now is: $now")
      println(s"Waiting  to: ${firstEvent.triggerOn}")
      actions.foreach(action => println(action))
      Thread.sleep(millis)
    }
  }
}

trait OrdonAction {
  def process(event: OrdonEvent, ordonOgameDriver: OrdonOgameDriver, eventRegistry: EventRegistry): List[OrdonAction]
}

abstract class BaseOrdonAction extends OrdonAction {
  final override def process(event: OrdonEvent, ordonOgameDriver: OrdonOgameDriver, eventRegistry: EventRegistry): List[OrdonAction] = {
    if (shouldHandleEvent(event)) {
      doProcess(event, ordonOgameDriver, eventRegistry)
    } else {
      List(this)
    }
  }

  def shouldHandleEvent(event: OrdonEvent): Boolean

  def doProcess(event: OrdonEvent, ordonOgameDriver: OrdonOgameDriver, eventRegistry: EventRegistry): List[OrdonAction]
}

abstract class EndlessOrdonAction extends BaseOrdonAction {
  final override def doProcess(event: OrdonEvent, ordonOgameDriver: OrdonOgameDriver, eventRegistry: EventRegistry): List[OrdonAction] = {
    eventRegistry.registerEvent(doProcessEndless(event, ordonOgameDriver))
    List(this)
  }

  def doProcessEndless(event: OrdonEvent, ordonOgameDriver: OrdonOgameDriver): OrdonEvent
}

abstract class TimeBasedOrdonAction extends EndlessOrdonAction {
  private var resumeOn: ZonedDateTime = null

  final def shouldHandleEvent(event: OrdonEvent): Boolean = {
    resumeOn == null || !event.triggerOn.isBefore(resumeOn)
  }

  final def doProcessEndless(event: OrdonEvent, ordonOgameDriver: OrdonOgameDriver): OrdonEvent = {
    resumeOn = processTimeBased(ordonOgameDriver)
    TimeBasedOrdonEvent(resumeOn)
  }

  def processTimeBased(ordonOgameDriver: OrdonOgameDriver): ZonedDateTime
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
