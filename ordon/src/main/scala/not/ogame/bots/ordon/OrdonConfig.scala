package not.ogame.bots.ordon

import cats.effect.IO
import not.ogame.bots.{Credentials, LocalClock}

trait OrdonConfig {
  def getCredentials: Credentials

  def getInitialActions(implicit clock: LocalClock): IO[List[ScheduledAction[IO]]]
}
