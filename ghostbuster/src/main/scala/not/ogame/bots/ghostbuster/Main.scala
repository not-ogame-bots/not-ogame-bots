package not.ogame.bots.ghostbuster

import cats.effect.concurrent.Ref
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global
import not.ogame.bots._
import not.ogame.bots.ghostbuster.actions.{CollectResourcesAction, SpreadResourcesAction}
import not.ogame.bots.ghostbuster.api.{CollectResourcesEndpoint, SpreadResourcesEndpoint, StatusEndpoint}
import not.ogame.bots.ghostbuster.executor.impl.TaskExecutorImpl
import not.ogame.bots.ghostbuster.infrastructure.{FCMService, FirebaseResource}
import not.ogame.bots.ghostbuster.interpreter.OgameActionInterpreterImpl
import not.ogame.bots.ghostbuster.notifications.OgameNotificationDecorator
import not.ogame.bots.ghostbuster.ogame.OgameActionDriver
import not.ogame.bots.ghostbuster.processors.{
  ActivityFakerProcessor,
  Builder,
  BuilderProcessor,
  EscapeFleetProcessor,
  ExpeditionProcessor,
  FlyAndBuildProcessor,
  FlyAndReturnProcessor
}
import not.ogame.bots.ghostbuster.reporting.{HostileFleetReporter, State, StateAggregator}
import not.ogame.bots.selenium.{SeleniumOgameDriverCreator, WebDriverResource}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import pureconfig.error.CannotConvert
import pureconfig.generic.auto._
import pureconfig.module.enumeratum._
import pureconfig.{ConfigObjectCursor, ConfigReader, ConfigSource}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._

import scala.concurrent.duration._

object Main extends StrictLogging {
  private implicit val clock: LocalClock = new RealLocalClock()
  private val SettingsDirectory = s"${System.getenv("HOME")}/.not-ogame-bots"

  def main(args: Array[String]): Unit = {
    Thread.setDefaultUncaughtExceptionHandler { (t, e) =>
      logger.error(s"Uncaught exception in thread: $t", e)
    }
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val botConfig = ConfigSource.resources("quasar.conf").loadOrThrow[BotConfig]
    val credentials = ConfigSource.file(s"${SettingsDirectory}/quasar-credentials.conf").loadOrThrow[Credentials]
    logger.info(pprint.apply(botConfig).render)

    Ref
      .of[Task, State](State.Empty)
      .flatMap(state => app(botConfig, credentials, state))
      .runSyncUnsafe()
  }

  private def app(botConfig: BotConfig, credentials: Credentials, state: Ref[Task, State]) = {
    val httpStateExposer = new StatusEndpoint(state)
    (for {
      driver <- WebDriverResource.firefox[Task]()
      selenium = SeleniumOgameDriverCreator.create[Task](driver, credentials)
      firebase <- FirebaseResource.create(SettingsDirectory)
      decoratedDriver = new OgameNotificationDecorator(selenium)
      executor = new TaskExecutorImpl(decoratedDriver)
      actionInterpreter = new OgameActionInterpreterImpl(decoratedDriver, executor)
      safeDriver = new OgameActionDriver
      collectingEndpoint = new CollectResourcesEndpoint(new CollectResourcesAction(safeDriver)(actionInterpreter))
      spreadingEndpoint = new SpreadResourcesEndpoint(new SpreadResourcesAction(safeDriver)(actionInterpreter))
      _ <- httpServer(List(httpStateExposer.getStatus, collectingEndpoint.collectEndpoint, spreadingEndpoint.spreadEndpoint))
    } yield (actionInterpreter, firebase, executor, safeDriver))
      .use {
        case (interpreter, firebase, executor, safeDriver) =>
          implicit val impInterpreter: OgameActionInterpreterImpl = interpreter
          val fcmService = new FCMService[Task](firebase)
          val stateAgg = new StateAggregator(state, interpreter)
          val hostileFleetReporter = new HostileFleetReporter(fcmService, interpreter)
          val builder = new Builder(safeDriver, botConfig.wishlist)
          val fbp = new FlyAndBuildProcessor(safeDriver, botConfig.fsConfig, builder)
          val ep = new ExpeditionProcessor(botConfig.expeditionConfig, safeDriver)
          val activityFaker = new ActivityFakerProcessor(safeDriver)
          val bp = new BuilderProcessor(builder, botConfig.smartBuilder, safeDriver)
          val far = new FlyAndReturnProcessor(botConfig.flyAndReturn, safeDriver)
          val efp = new EscapeFleetProcessor(safeDriver, botConfig.escapeConfig)
          Task.raceMany(
            List(
              executor.run(),
              fbp.run(),
              activityFaker.run(),
              ep.run(),
              bp.run(),
              far.run(),
              stateAgg.run(),
              hostileFleetReporter.run(),
              efp.run()
            )
          )
      }
      .onErrorRestartIf { e =>
        logger.error(e.getMessage, e)
        true
      }
  }

  private def httpServer(endpoints: List[ServerEndpoint[_, _, _, Nothing, Task]]) = {
    BlazeServerBuilder[Task](Scheduler.io())
      .withIdleTimeout(240 seconds)
      .withResponseHeaderTimeout(240 seconds)
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(
        Router("/" -> endpoints.toRoutes).orNotFound
      )
      .resource
      .void
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
