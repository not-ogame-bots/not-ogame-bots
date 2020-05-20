package not.ogame.bots.ghostbuster

import java.time.Clock

import cats.effect.{ExitCode, IO, IOApp}
import not.ogame.bots.{Credentials, OgameDriver}
import not.ogame.bots.selenium.SeleniumOgameDriverCreator
import pureconfig.{ConfigObjectCursor, ConfigReader, ConfigSource}
import pureconfig.generic.auto._
import eu.timepit.refined.pureconfig._
import pureconfig.module.enumeratum._
import pureconfig.{ConfigObjectCursor, ConfigReader}
import cats.implicits._
import pureconfig.error.CannotConvert

import scala.concurrent.duration._

object Main extends IOApp {
  private implicit val clock: Clock = Clock.systemUTC()

  override def run(args: List[String]): IO[ExitCode] = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val botConfig = ConfigSource.default.loadOrThrow[BotConfig]
    val credentials = ConfigSource.file(s"${System.getenv("HOME")}/.not-ogame-bots/credentials.conf").loadOrThrow[Credentials]
    val gbot = new GBot(RealRandomTimeJitter)
    new SeleniumOgameDriverCreator()
      .create(credentials)
      .use { ogame =>
        val taskExecutor = new TaskExecutor[IO](ogame)
        def loop(state: State): IO[State] = {
          val ns = gbot.nextStep(state)
          taskExecutor.execute(ns).flatMap(s => IO.sleep(1 seconds) >> loop(s))
        }
        loop(State.LoggedOut(List.empty, botConfig.wishlist))
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

  implicit val buildWishReader: ConfigReader[Wish.Build] = ConfigReader.forProduct2("suppliesBuilding", "level")(Wish.Build)

  def extractByType(typ: String, objCur: ConfigObjectCursor): ConfigReader.Result[Wish] = typ match {
    case "build" => buildWishReader.from(objCur)
    case t =>
      objCur.failed(CannotConvert(objCur.value.toString, "Identifiable", s"type has value $t instead of build"))
  }
}
