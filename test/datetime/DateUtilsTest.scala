package datetime

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, Matchers}
import DateUtils._

class DateUtilsTest extends FreeSpec with Matchers {

  "daysAgo" - {
    "should correctly calculate how long ago a date was" in {
      daysAgo(DateTime.now.minusDays(5)) shouldEqual 5
    }
  }

  "periodStatus" - {
    "should mark 500 as status red" in {
      periodStatus(500) shouldEqual Some("Red")
    }

    "should return none for negative values" in {
      periodStatus(-50) shouldEqual None
    }
  }
}
