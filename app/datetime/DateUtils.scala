package datetime

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.libs.json.{Format, Writes, Reads}

object DateUtils {
  implicit val isoDateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  implicit val isoDateWrites = Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  implicit val jodaDateTimeFormats = Format[DateTime](isoDateReads, isoDateWrites)
  val yearMonthDay: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  val yearMonthDayTime: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
}
