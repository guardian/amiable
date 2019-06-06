package utils

import models.{Age, Fresh, Old, Turning}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, Days}
import play.api.libs.json._

object DateUtils {

  val freshnessLimit = 30

  def daysAgo(date:DateTime): Int = Days.daysBetween(date, DateTime.now).getDays

  def getAgeColour(date: DateTime): String = {
    getAge(date).fold("black"){
      case Fresh => "green"
      case Turning => "amber"
      case Old => "red"
    }
  }

  def getAge(date: DateTime): Option[Age] = {
    daysAgo(date) match {
      case age if age < 0 => None
      case age if age < 14 => Some(Fresh)
      case age if age < freshnessLimit => Some(Turning)
      case _ => Some(Old)
    }
  }

  implicit val isoDateReads: Reads[DateTime] = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  implicit val isoDateWrites: Writes[DateTime] = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  implicit val jodaDateTimeFormats: Format[DateTime] = Format[DateTime](isoDateReads, isoDateWrites)

  val yearMonthDay: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  val yearMonthDayTime: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
  val readableDateTime: DateTimeFormatter = DateTimeFormat.forPattern("dd MMMM yyyy 'at' HH:mm")
}
