package not.ogame.bots.ghostbuster

import java.time.Clock

import eu.timepit.refined.pureconfig._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import not.ogame.bots.Credentials
import not.ogame.bots.ghostbuster.executor.TaskExecutorImpl
import not.ogame.bots.ghostbuster.processors.{ActivityFakerProcessor, BuilderProcessor, ExpeditionProcessor, FlyAndBuildProcessor}
import not.ogame.bots.selenium.SeleniumOgameDriverCreator
import pureconfig.error.CannotConvert
import pureconfig.generic.auto._
import pureconfig.module.enumeratum._
import pureconfig.{ConfigObjectCursor, ConfigReader, ConfigSource}

object Main {
  private implicit val clock: Clock = Clock.systemUTC()

  def main(args: Array[String]): Unit = {
    Thread.setDefaultUncaughtExceptionHandler { (t, e) =>
      println("Uncaught exception in thread: " + t)
      e.printStackTrace()
    }
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val botConfig = ConfigSource.default.loadOrThrow[BotConfig]
    val credentials = ConfigSource.file(s"${System.getenv("HOME")}/.not-ogame-bots/credentials.conf").loadOrThrow[Credentials]

    new SeleniumOgameDriverCreator[Task]()
      .create(credentials)
      .use { ogame =>
        val taskExecutor = new TaskExecutorImpl(ogame, clock)
        val fbp = new FlyAndBuildProcessor(taskExecutor, botConfig, clock)
        val ep = new ExpeditionProcessor(botConfig, taskExecutor)
        val activityFaker = new ActivityFakerProcessor(taskExecutor)
        val bp = new BuilderProcessor(botConfig, taskExecutor)
        Task.raceMany(List(taskExecutor.run(), fbp.run(), activityFaker.run(), ep.run(), bp.run()))
      }
      .runSyncUnsafe()
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
    case "smart_builder"  => implicitly[ConfigReader[Wish.SmartSupplyBuilder]].from(objCur)
    case "research"       => implicitly[ConfigReader[Wish.Research]].from(objCur)
    case t =>
      objCur.failed(CannotConvert(objCur.value.toString, "Wish", s"unknown type: $t"))
  }
}
