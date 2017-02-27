package metrics

import models.{Chart, ChartTimeSerie}
import org.joda.time.DateTime
import utils.DateUtils

object Charts {

  def charts(instanceCountHistory: List[(DateTime, Double)],
             age25thPercentileHistory: List[(DateTime, Double)],
             age50thPercentileHistory: List[(DateTime, Double)],
             age75thPercentileHistory: List[(DateTime, Double)]
            ) : List[Chart] = {
    List(
      Chart("Instances with out-of-date AMI", List(ChartTimeSerie("Old instances count", instanceCountHistory))),
      Chart("Age of AMIs (Percentiles)",
        List(
          ChartTimeSerie("25th percentile", age25thPercentileHistory, "#4d94ff"),
          ChartTimeSerie("50th percentile", age50thPercentileHistory, "#3385ff"),
          ChartTimeSerie("75th percentile", age75thPercentileHistory, "#1a75ff"),
          ChartTimeSerie("Freshness goal", age25thPercentileHistory.map{ case (d, v) => (d, DateUtils.freshnessLimit.toDouble) }, "green")
        )
      )
    )
  }

}
