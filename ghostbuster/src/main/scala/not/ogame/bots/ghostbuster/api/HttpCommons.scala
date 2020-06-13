package not.ogame.bots.ghostbuster.api

import com.softwaremill.tagging.@@
import io.circe.Printer
import sttp.tapir.{Schema, Tapir, Validator}
import sttp.tapir.json.circe.TapirJsonCirce

trait HttpCommons extends Tapir with TapirJsonCirce with JsonCodecs {
  override def jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)

  implicit def taggedStringSchema[T]: Schema[String @@ T] = Schema.schemaForString.asInstanceOf[Schema[String @@ T]]
  implicit def taggedStringValidator[T]: Validator[String @@ T] = Validator.pass[String @@ T]
}
