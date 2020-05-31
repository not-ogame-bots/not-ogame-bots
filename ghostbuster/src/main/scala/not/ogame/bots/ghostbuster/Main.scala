package not.ogame.bots.ghostbuster

import cats.effect.concurrent.Ref
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import eu.timepit.refined.pureconfig._
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global
import not.ogame.bots.ghostbuster.executor.TaskExecutorImpl
import not.ogame.bots.ghostbuster.infrastructure.FCMService
import not.ogame.bots.ghostbuster.processors.{ActivityFakerProcessor, Builder, BuilderProcessor, ExpeditionProcessor, FlyAndBuildProcessor}
import not.ogame.bots.ghostbuster.reporting.{HostileFleetReporter, State, StateAggregator, StateListenerDispatcher}
import not.ogame.bots.selenium.SeleniumOgameDriverCreator
import not.ogame.bots.{Credentials, Fleet, LocalClock, PlanetId, RealLocalClock}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import pureconfig.error.CannotConvert
import pureconfig.generic.auto._
import pureconfig.module.enumeratum._
import pureconfig.{ConfigObjectCursor, ConfigReader, ConfigSource}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._

object Main extends StrictLogging {
  private implicit val clock: LocalClock = new RealLocalClock()
  private val SettingsDirectory = s"${System.getenv("HOME")}/.not-ogame-bots/"

  def main(args: Array[String]): Unit = {
    Thread.setDefaultUncaughtExceptionHandler { (t, e) =>
      logger.error(s"Uncaught exception in thread: $t", e)
    }
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val botConfig = ConfigSource.default.loadOrThrow[BotConfig]
    val credentials = ConfigSource.file(s"${SettingsDirectory}credentials.conf").loadOrThrow[Credentials]
    logger.info(pprint.apply(botConfig).render)

    Ref
      .of[Task, State](State.Empty)
      .flatMap { state =>
        val httpStateExposer = new StatusEndpoint(state)
        Task.parMap2(selenium(botConfig, credentials, state), httpServer(httpStateExposer.getStatus))((_, _) => ())
      }
      .runSyncUnsafe()
  }

  private def httpServer(endpoint: ServerEndpoint[Unit, Unit, State, Nothing, Task]) = {
    BlazeServerBuilder[Task](Scheduler.io())
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(
        Router("/" -> endpoint.toRoutes).orNotFound
      )
      .resource
      .void
      .use(_ => Task.never[Unit])
  }

  private def selenium(botConfig: BotConfig, credentials: Credentials, state: Ref[Task, State]) = {
    new SeleniumOgameDriverCreator[Task]()
      .create(credentials)
      .use { ogame =>
        val stateAgg = new StateAggregator[Task](state)
        val seenFleetsState = Ref[Task].of(Set.empty[Fleet]).runSyncUnsafe()
        val fcmService = new FCMService[Task](SettingsDirectory)
        fcmService.initializeFirebase()
        val hostileFleetReporter = new HostileFleetReporter[Task](fcmService, seenFleetsState)
        val taskExecutor = new TaskExecutorImpl(ogame, clock, new StateListenerDispatcher[Task](List(stateAgg, hostileFleetReporter)))
        val builder = new Builder(taskExecutor, botConfig.wishlist)
        val fbp = new FlyAndBuildProcessor(taskExecutor, botConfig.fsConfig, builder)
        val ep = new ExpeditionProcessor(botConfig.expeditionConfig, taskExecutor)
        val activityFaker = new ActivityFakerProcessor(taskExecutor)
        val bp = new BuilderProcessor(builder, botConfig.smartBuilder, taskExecutor)
        Task.raceMany(List(taskExecutor.run(), fbp.run(), activityFaker.run(), ep.run(), bp.run()))
      }
      .onErrorRestartIf { e =>
        logger.error(e.getMessage, e)
        true
      }
  }

  implicit val wishReader: ConfigReader[Wish] = ConfigReader.fromCursor { cur =>
    for {
      objCur <- cur.asObjectCursor
      typeCur <- objCur.atKey("type")
      typeStr <- typeCur.asString
      ident <- extractByType(typeStr, objCur)
    } yield ident
  }

  implicit def taggedStringReader(implicit cr: ConfigReader[String]): ConfigReader[PlanetId] = {
    cr.map(PlanetId.apply)
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
