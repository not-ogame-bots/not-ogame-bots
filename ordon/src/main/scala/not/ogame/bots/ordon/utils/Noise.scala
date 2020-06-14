package not.ogame.bots.ordon.utils

import java.awt.Toolkit

object Noise {
  def makeNoise(): Unit = {
    for (_ <- 1 to 30) {
      Toolkit.getDefaultToolkit.beep()
      Thread.sleep(100)
    }
  }
}
