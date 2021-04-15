package metrics

import com.amazonaws.services.cloudwatch.model.{Datapoint, GetMetricStatisticsResult}
import org.joda.time.DateTime
import org.scalatest.{FreeSpec, Matchers, OptionValues}

import scala.collection.JavaConverters._


class CloudWatchTest extends FreeSpec with Matchers with OptionValues {
  val cloudwatch = new CloudWatch("TEST")
  "putRequest" - {
    "sets provided count value" in {
      val metricDataRequest = cloudwatch.putRequest("test-metric", 5)
      val metricDatum = metricDataRequest.getMetricData.asScala.headOption.value
      metricDatum.getValue shouldEqual 5
    }
  }

  "extractCountRequestData" - {
    val dateTime = new DateTime(2016, 4, 11, 0, 0)
    val value = 10.0
    val result = new GetMetricStatisticsResult()
      .withDatapoints {
        new Datapoint()
          .withMaximum(value)
          .withTimestamp(dateTime.toDate)
      }

    "extracts date from result" in {
      val (dt, _) = cloudwatch.extractDataFromResult(result).headOption.value
      dt shouldEqual dateTime
    }

    "extracts max value from result" in {
      val (_, v) = cloudwatch.extractDataFromResult(result).headOption.value
      v shouldEqual value
    }
  }
}
