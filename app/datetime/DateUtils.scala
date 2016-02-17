package datetime

import play.api.Logger
import org.joda.time.{Days, DateTime}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.libs.json.{Format, Writes, Reads}

//case class DateWithWarning(dateString: String, status: PeriodStatus)

object DateUtils {
  def getAgeColour(date: DateTime) = {
    val days = daysAgo(date)
    if (days < 0 ) {
      Logger.error("AMI cannot be from the future!")
      "black"
    }
    else {
      if (days < 7) "green"
      else if (days < 30) "amber"
      else "red"
    }
  }

  implicit val isoDateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  implicit val isoDateWrites = Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  implicit val jodaDateTimeFormats = Format[DateTime](isoDateReads, isoDateWrites)
  val yearMonthDay: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  val yearMonthDayTime: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
  val readableDateTime: DateTimeFormatter = DateTimeFormat.forPattern("dd MMMM yyyy 'at' HH:mm")

  def daysAgo(date:DateTime): Int = Days.daysBetween(date, DateTime.now).getDays

}
