package metrics

import aws.AwsAsyncHandler.awsToScala
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{
  AWSCredentialsProviderChain,
  InstanceProfileCredentialsProvider
}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder
import com.amazonaws.services.cloudwatch.model._
import config.AmiableConfigProvider
import models.Attempt
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logging
import play.api.mvc.Handler.Stage
import services.OldInstanceAccountHistory

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

sealed abstract class CloudWatchMetric(val name: String)
object CloudWatchMetrics {
  case object OldCount
      extends CloudWatchMetric("instances-running-out-of-date-amis")
  case object AmisAgePercentile25th
      extends CloudWatchMetric("instances-amis-age-percentile-25th")
  case object AmisAgePercentile50th
      extends CloudWatchMetric("instances-amis-age-percentile-50th")
  case object AmisAgePercentile75th
      extends CloudWatchMetric("instances-amis-age-percentile-75th")
  case object AmisAgePercentile90th
      extends CloudWatchMetric("instances-amis-age-percentile-90th")
  case object AmisAgePercentileHighest
      extends CloudWatchMetric("instances-amis-age-percentile-highest")
  case object OldCountByAccount extends CloudWatchMetric("Vulnerabilities")
}

class CloudWatch() extends Logging {
  lazy val client = {
    val credentialsProvider = new AWSCredentialsProviderChain(
      InstanceProfileCredentialsProvider.getInstance(),
      new ProfileCredentialsProvider("deployTools")
    )
    val region = Option(Regions.getCurrentRegion).getOrElse(
      Region.getRegion(Regions.EU_WEST_1)
    )
    val acwac = AmazonCloudWatchAsyncClientBuilder
      .standard()
      .withCredentials(credentialsProvider)
      .withRegion(region.getName)
      .build()
    acwac
  }

  private[metrics] def putRequest(
      namespace: String,
      metricName: String,
      value: Int,
      dimensions: List[Dimension] = List.empty
  ): PutMetricDataRequest = {
    new PutMetricDataRequest()
      .withNamespace(namespace)
      .withMetricData {
        new MetricDatum()
          .withMetricName(metricName)
          .withValue(value.toDouble)
          .withDimensions(dimensions.asJava)
      }
  }

  private def getRequest(
      namespace: String,
      metricName: String
  ): GetMetricStatisticsRequest = {
    val now = DateTime.now(DateTimeZone.UTC)
    new GetMetricStatisticsRequest()
      .withNamespace(namespace)
      .withMetricName(metricName)
      .withPeriod(60 * 60 * 24) // 1 day (24 hrs)
      .withStartTime(now.minusDays(90).toDate)
      .withEndTime(now.toDate)
      .withStatistics(Statistic.Maximum)
  }

  private[metrics] def extractDataFromResult(
      result: GetMetricStatisticsResult
  ): List[(DateTime, Double)] = {
    result.getDatapoints.asScala.toList
      .map { dp =>
        new DateTime(dp.getTimestamp) -> dp.getMaximum.toDouble
      }
      .sortBy(_._1.getMillis)
  }

  private def getWithRequest(request: GetMetricStatisticsRequest)(implicit
      executionContext: ExecutionContext
  ): Future[List[(DateTime, Double)]] = {
    awsToScala(client.getMetricStatisticsAsync)(request)
      .map(extractDataFromResult)
  }

  private def putWithRequest(request: PutMetricDataRequest) = {
    awsToScala(client.putMetricDataAsync)(request)
  }

  def get(namespace: String, metricName: String)(implicit
      executionContext: ExecutionContext
  ): Attempt[Option[List[(DateTime, Double)]]] = {
    Attempt.fromFuture(
      getWithRequest(getRequest(namespace, metricName)).map(ds =>
        Right(Option(ds))
      )
    ) { case e =>
      logger.warn("Failed to fetch CloudWatch data", e)
      Right(None)
    }
  }

  def put(
      namespace: String,
      metricName: String,
      maybeValue: Option[Int]
  ): Unit = {
    maybeValue.fold {
      logger.warn(
        s"Not updating CloudWatch - no value available for '$metricName'"
      )
    } { value =>
      putWithRequest(putRequest(namespace, metricName, value))
      logger.debug(
        s"Updated CloudWatch metric '$metricName' with value '$value'"
      )
    }
  }

  def put(
      namespace: String,
      metricName: String,
      oldInstanceAccountHistory: List[OldInstanceAccountHistory]
  ): Unit = {
    oldInstanceAccountHistory.foreach { oldAMICount =>
      val dimensions = List(
        new Dimension().withName("Account").withValue(oldAMICount.accountName),
        new Dimension().withName("DataType").withValue("oldami/total")
      )
      putWithRequest(
        putRequest(namespace, metricName, oldAMICount.count, dimensions)
      )
    }
  }
}
