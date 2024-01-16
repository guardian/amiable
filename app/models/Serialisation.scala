package models

import play.api.libs.json.{Format, Json}

object Serialisation {
  import utils.DateUtils._
  import play.api.libs.json.JodaWrites._
  import play.api.libs.json.JodaReads._

  implicit val originFormat: Format[Origin] = Json.format[Origin]
  implicit val metaFormat: Format[Meta] = Json.format[Meta]
  implicit val instanceFormat: Format[Instance] = Json.format[Instance]

}
