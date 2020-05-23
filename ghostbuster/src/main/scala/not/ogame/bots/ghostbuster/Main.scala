package not.ogame.bots.ghostbuster

import java.time.Clock

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import eu.timepit.refined.pureconfig._
import not.ogame.bots.Credentials
import not.ogame.bots.selenium.SeleniumOgameDriverCreator
import pureconfig.error.CannotConvert
import pureconfig.generic.auto._
import pureconfig.module.enumeratum._
import pureconfig.{ConfigObjectCursor, ConfigReader, ConfigSource}

import scala.concurrent.duration._

object Main extends IOApp {
  private implicit val clock: Clock = Clock.systemUTC()

  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val botConfig = ConfigSource.default.loadOrThrow[BotConfig]
    val credentials = ConfigSource.file(s"${System.getenv("HOME")}/.not-ogame-bots/credentials.conf").loadOrThrow[Credentials]
    val gbot = new GBot(RealRandomTimeJitter, botConfig)
    new SeleniumOgameDriverCreator()
      .create(credentials)
      .use { ogame =>
        val taskExecutor = new TaskExecutor[IO](ogame, gbot)

        def loop(state: State): IO[PlanetState] = {
          taskExecutor.execute(state).flatMap(s => IO.sleep(1 second) >> loop(s))
        }

        loop(State.LoggedOut(List.empty))
      }
      .as(ExitCode.Success)
  }

  implicit val wishReader: ConfigReader[Wish] = ConfigReader.fromCursor { cur =>
    for {
      objCur <- cur.asObjectCursor
      typeCur <- objCur.atKey("type")
      typeStr <- typeCur.asString
      ident <- extractByType(typeStr, objCur)
    } yield ident
  }

  def extractByType(typ: String, objCur: ConfigObjectCursor): ConfigReader.Result[Wish] = typ match {
    case "build_supply"   => implicitly[ConfigReader[Wish.BuildSupply]].from(objCur)
    case "build_facility" => implicitly[ConfigReader[Wish.BuildFacility]].from(objCur)
    case "build_ship"     => implicitly[ConfigReader[Wish.BuildShip]].from(objCur)
    case t =>
      objCur.failed(CannotConvert(objCur.value.toString, "Wish", s"unknown type: $t"))
  }
}
