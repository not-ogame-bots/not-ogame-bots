package not.ogame.bots.ghostbuster

import scala.util.Random

object RealRandomTimeJitter extends RandomTimeJitter {
  private val random = new Random()

  override def getJitterInSeconds(): Int = random.nextInt(20) + 1
}
