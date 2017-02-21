package utils

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, Matchers}
import DateUtils._

class DateUtilsTest extends FreeSpec with Matchers {
  "daysAgo" - {
    "should correctly calculate how long ago a date was" in {
      daysAgo(DateTime.now.minusDays(5)) shouldEqual 5
    }
  }

  "getAgeColour" - {
    "old dates are red" in {
      getAgeColour(DateTime.now().minusDays(50)) shouldEqual "red"
    }

    "dates of a medium age are amber" in {
      getAgeColour(DateTime.now().minusDays(14)) shouldEqual "amber"
    }

    "very recent dates are green" in {
      getAgeColour(DateTime.now().minusDays(1)) shouldEqual "green"
    }
  }
}
