package not.ogame.bots.ordon.core

import cats.implicits._
import not.ogame.bots._
import not.ogame.bots.ordon.OrdonQuasarConfig

object Boot extends OrdonMainLoop {
  override val credentials: Credentials = OrdonQuasarConfig.getCredentials
  override val actions: List[OrdonAction] = OrdonQuasarConfig.initialActionsV2()
}
