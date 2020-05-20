package not.ogame.bots.ghostbuster

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.pureconfig._
import not.ogame.bots.{SuppliesBuilding, SuppliesPageData}
import pureconfig.error.CannotConvert
import pureconfig.generic.auto._
import pureconfig.module.enumeratum._
import pureconfig.{ConfigObjectCursor, ConfigReader}

sealed trait Task {
  def executeAfter: Instant

  def isFromWish(wish: Wish): Boolean
}
object Task {
  case class Build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, executeAfter: Instant) extends Task {
    override def isFromWish(wish: Wish): Boolean = {
      wish match {
        case Wish.Build(sb, l) => sb == suppliesBuilding && l == level
      }
    }
  }
  case class Login(executeAfter: Instant) extends Task {
    override def isFromWish(wish: Wish): Boolean = false
  }
  case class Refresh(executeAfter: Instant) extends Task {
    override def isFromWish(wish: Wish): Boolean = false
  }

  def build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive, executeAfter: Instant): Task = {
    Build(suppliesBuilding, level, executeAfter)
  }

  def login(executeAfter: Instant): Task = Task.Login(executeAfter)

  def refresh(executeAfter: Instant): Task = Task.Refresh(executeAfter)
}

sealed trait Wish
object Wish {
  case class Build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive) extends Wish

  def build(suppliesBuilding: SuppliesBuilding, level: Int Refined Positive): Wish = Build(suppliesBuilding, level)

  implicit val wishReader: ConfigReader[Wish] = ConfigReader.fromCursor { cur =>
    for {
      objCur <- cur.asObjectCursor
      typeCur <- objCur.atKey("type")
      typeStr <- typeCur.asString
      ident <- extractByType(typeStr, objCur)
    } yield ident
  }

  val buildWishReader: ConfigReader[Wish.Build] = ConfigReader.forProduct2("suppliesBuilding", "level")(Wish.Build)

  def extractByType(typ: String, objCur: ConfigObjectCursor): ConfigReader.Result[Wish] = typ match {
    case "build" => buildWishReader.from(objCur)
    case t =>
      objCur.failed(CannotConvert(objCur.value.toString, "Identifiable", s"type has value $t instead of build"))
  }
}

sealed trait State {
  def scheduledTasks: List[Task]
  def wishList: List[Wish]
}
object State {
  case class LoggedOut(scheduledTasks: List[Task], wishList: List[Wish]) extends State
  case class LoggedIn(suppliesPage: SuppliesPageData, wishList: List[Wish], scheduledTasks: List[Task]) extends State

  def loggedIn(suppliesPage: SuppliesPageData, wishList: List[Wish], scheduledTasks: List[Task]): State = {
    State.LoggedIn(suppliesPage, wishList, scheduledTasks)
  }
}

case class BotConfig(wishlist: List[Wish])
