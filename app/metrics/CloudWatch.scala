package metrics

import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  DefaultCredentialsProvider
}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model._
import config.AmiableConfigProvider
import models.Attempt
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logging
import play.api.mvc.Handler.Stage
import services.OldInstanceAccountHistory

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters._

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
    import software.amazon.awssdk.auth.credentials.{AwsCredentialsProviderChain, ProfileCredentialsProvider, InstanceProfileCredentialsProvider}
    
    val profileProvider = ProfileCredentialsProvider.builder()
      .profileName("deployTools")
      .build()
    
    val instanceProvider = InstanceProfileCredentialsProvider.builder()
      .build()
    
    val credentialsProvider: AwsCredentialsProvider = AwsCredentialsProviderChain.builder()
      .addCredentialsProvider(instanceProvider)
      .addCredentialsProvider(profileProvider)
      .build()
      
    val region = Region.EU_WEST_1
    
    CloudWatchAsyncClient.builder()
      .credentialsProvider(credentialsProvider)
      .region(region)
      .build()
  }

  private[metrics] def putRequest(
      namespace: String,
      metricName: String,
      value: Int,
      dimensions: List[Dimension] = List.empty
  ): PutMetricDataRequest = {
    PutMetricDataRequest.builder()
      .namespace(namespace)
      .metricData(
        MetricDatum.builder()
          .metricName(metricName)
          .value(value.toDouble)
          .dimensions(dimensions.asJava)
          .build()
      )
      .build()
  }

  private def getRequest(
      namespace: String,
      metricName: String
  ): GetMetricStatisticsRequest = {
    val now = DateTime.now(DateTimeZone.UTC)
    GetMetricStatisticsRequest.builder()
      .namespace(namespace)
      .metricName(metricName)
      .period(60 * 60 * 24) // 1 day (24 hrs)
      .startTime(now.minusDays(90).toDate.toInstant)
      .endTime(now.toDate.toInstant)
      .statistics(Statistic.MAXIMUM)
      .build()
  }

  private[metrics] def extractDataFromResult(
      result: GetMetricStatisticsResponse
  ): List[(DateTime, Double)] = {
    result.datapoints().asScala.toList
      .map { dp =>
        new DateTime(dp.timestamp().toEpochMilli) -> dp.maximum().doubleValue()
      }
      .sortBy(_._1.getMillis)
  }

  private def getWithRequest(request: GetMetricStatisticsRequest)(implicit
      executionContext: ExecutionContext
  ): Future[List[(DateTime, Double)]] = {
    client.getMetricStatistics(request).asScala
      .map(extractDataFromResult)
  }

  private def putWithRequest(request: PutMetricDataRequest) = {
    client.putMetricData(request).asScala
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
        Dimension.builder().name("Account").value(oldAMICount.accountName).build(),
        Dimension.builder().name("DataType").value("oldami/total").build()
      )
      putWithRequest(
        putRequest(namespace, metricName, oldAMICount.count, dimensions)
      )
    }
  }
}
