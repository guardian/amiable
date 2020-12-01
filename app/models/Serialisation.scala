package models

import play.api.libs.json.Json

object Serialisation {
  import utils.DateUtils._
  import play.api.libs.json.JodaWrites._
  import play.api.libs.json.JodaReads._

  implicit val originFormat = Json.format[Origin]
  implicit val metaFormat = Json.format[Meta]
  implicit val instanceFormat = Json.format[Instance]

}
