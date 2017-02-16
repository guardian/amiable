package utils

import org.scalatest.{FreeSpec, Matchers}

import scala.util.Random

class StatsTests extends FreeSpec with Matchers {

  val percentiles = Percentiles(Random.shuffle(0 to 100).toSeq)

  "Percentiles" - {
    "should correctly calculate 95th percentile" in {
      percentiles.p95 shouldEqual 95
    }
    "should correctly calculate 90th percentile" in {
      percentiles.p90 shouldEqual 90
    }
    "should correctly calculate 75th percentile" in {
      percentiles.p75 shouldEqual 75
    }
    "should correctly calculate 50th percentile" in {
      percentiles.p50 shouldEqual 50
    }
    "should correctly calculate 25th percentile" in {
      percentiles.p25 shouldEqual 25
    }
    "should correctly calculate lowest value" in {
      percentiles.lowest shouldEqual 0
    }
    "should correctly calculate highest value" in {
      percentiles.highest shouldEqual 100
    }
  }
}

