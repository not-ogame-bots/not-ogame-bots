package not.ogame.bots.ordon

import cats.effect.IO
import not.ogame.bots.{Credentials, LocalClock}

object OrdonTestConfig extends OrdonConfig {
  override def getCredentials: Credentials = Credentials(
    login = "fire@fire.pl",
    password = "1qaz2wsx",
    universeName = "Norma",
    universeId = "s166-pl"
  )

  override def getInitialActions(implicit clock: LocalClock): IO[List[ScheduledAction[IO]]] = OrdonQuasarConfig.getInitialActions(clock)
}
