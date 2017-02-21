package utils

import org.scalatest.{FreeSpec, Matchers}

import scala.util.Random

class StatsTests extends FreeSpec with Matchers {

  val percentiles = Percentiles(Random.shuffle(0 to 100).toSeq)

  "Percentiles" - {
    "should return None if no values" in {
      Percentiles(Seq.empty[Int]).p25 shouldEqual None
    }

    "should correctly calculate 95th percentile" in {
      percentiles.p95 shouldEqual Some(95)
    }
    "should correctly calculate 90th percentile" in {
      percentiles.p90 shouldEqual Some(90)
    }
    "should correctly calculate 75th percentile" in {
      percentiles.p75 shouldEqual Some(75)
    }
    "should correctly calculate 50th percentile" in {
      percentiles.p50 shouldEqual Some(50)
    }
    "should correctly calculate 25th percentile" in {
      percentiles.p25 shouldEqual Some(25)
    }
    "should correctly calculate lowest value" in {
      percentiles.lowest shouldEqual Some(0)
    }
    "should correctly calculate highest value" in {
      percentiles.highest shouldEqual Some(100)
    }
  }
}

