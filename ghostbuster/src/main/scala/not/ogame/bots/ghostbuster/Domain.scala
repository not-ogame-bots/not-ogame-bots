package not.ogame.bots.ghostbuster

import not.ogame.bots.ghostbuster.processors.{
  EscapeConfig,
  ExpeditionConfig,
  ExpeditionDebrisCollectorConfig,
  FlyAndReturnConfig,
  FsConfig,
  SmartBuilderConfig,
  Wish
}

case class BotConfig(
    wishlist: List[Wish],
    fsConfig: FsConfig,
    expeditionConfig: ExpeditionConfig,
    smartBuilder: SmartBuilderConfig,
    escapeConfig: EscapeConfig,
    flyAndReturn: FlyAndReturnConfig,
    expeditionDebrisCollectorConfig: ExpeditionDebrisCollectorConfig
)
