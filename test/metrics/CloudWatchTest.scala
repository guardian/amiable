package metrics

import software.amazon.awssdk.services.cloudwatch.model.{
  Datapoint,
  GetMetricStatisticsResponse
}
import org.joda.time.DateTime
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.OptionValues

import scala.collection.JavaConverters._

class CloudWatchTest extends AnyFreeSpec with Matchers with OptionValues {
  val cloudwatch = new CloudWatch()
  "putRequest" - {
    "sets provided count value" in {
      val metricDataRequest =
        cloudwatch.putRequest("test-namespace", "test-metric", 5)
      val metricDatum = metricDataRequest.metricData().asScala.headOption.value
      metricDatum.value() shouldEqual 5
    }
  }

  "extractCountRequestData" - {
    val dateTime = new DateTime(2016, 4, 11, 0, 0)
    val value = 10.0
    val result = GetMetricStatisticsResponse.builder()
      .datapoints(
        Datapoint.builder()
          .maximum(value)
          .timestamp(dateTime.toDate.toInstant)
          .build()
      )
      .build()

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
