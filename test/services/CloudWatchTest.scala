package services

import org.scalatest.{OptionValues, Matchers, FreeSpec}
import collection.JavaConverters._


class CloudWatchTest extends FreeSpec with Matchers with OptionValues {
  "putOldCountRequest" - {
    "sets provided count value" in {
      val metricDataRequest = CloudWatch.putOldCountRequest(5)
      val metricDatum = metricDataRequest.getMetricData.asScala.headOption.value
      metricDatum.getValue shouldEqual 5
    }

    "sets stack dimension with '*'" in {
      val metricDataRequest = CloudWatch.putOldCountRequest(5)
      val dimensions = metricDataRequest.getMetricData.asScala.headOption.value.getDimensions.asScala
      val stageDimension = dimensions.find(_.getName == "stack").value
      stageDimension.getValue shouldEqual "*"
    }

    "sets stage dimension with 'PROD'" in {
      val metricDataRequest = CloudWatch.putOldCountRequest(5)
      val dimensions = metricDataRequest.getMetricData.asScala.headOption.value.getDimensions.asScala
      val stageDimension = dimensions.find(_.getName == "stage").value
      stageDimension.getValue shouldEqual "PROD"
    }
  }
}
