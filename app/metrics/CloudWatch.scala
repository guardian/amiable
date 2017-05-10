package metrics

import aws.AwsAsyncHandler.awsToScala
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder
import com.amazonaws.services.cloudwatch.model._
import models.Attempt
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}


sealed abstract class CloudWatchMetric(val name: String)
object CloudWatchMetrics {
  case object OldCount extends CloudWatchMetric("instances-running-out-of-date-amis")
  case object AmisAgePercentile25th extends CloudWatchMetric("instances-amis-age-percentile-25th")
  case object AmisAgePercentile50th extends CloudWatchMetric("instances-amis-age-percentile-50th")
  case object AmisAgePercentile75th extends CloudWatchMetric("instances-amis-age-percentile-75th")
  case object AmisAgePercentile90th extends CloudWatchMetric("instances-amis-age-percentile-90th")
  case object AmisAgePercentileHighest extends CloudWatchMetric("instances-amis-age-percentile-highest")
}

object CloudWatch {
  lazy val client = {
    val credentialsProvider = new AWSCredentialsProviderChain(
      InstanceProfileCredentialsProvider.getInstance(),
      new ProfileCredentialsProvider("deployTools")
    )
    val region = Option(Regions.getCurrentRegion).getOrElse(Region.getRegion(Regions.EU_WEST_1))
    val acwac = AmazonCloudWatchAsyncClientBuilder.standard()
      .withCredentials(credentialsProvider)
      .withRegion(region.getName).build()
    acwac
  }

  val prodStage = "PROD"
  val allStacks = "*"
  val namespace = "AMIs"

  private val dimensions = List(
    new Dimension()
      .withName("stage")
      .withValue(prodStage),
    new Dimension()
      .withName("stack")
      .withValue(allStacks)
  )

  private[metrics] def putRequest(metricName: String, value: Int): PutMetricDataRequest = {
    new PutMetricDataRequest()
      .withNamespace(namespace)
      .withMetricData {
        new MetricDatum()
          .withMetricName(metricName)
          .withValue(value.toDouble)
          .withDimensions(dimensions.asJava)
      }
  }

  private def getRequest(metricName: String): GetMetricStatisticsRequest = {
    val now = DateTime.now(DateTimeZone.UTC)
    new GetMetricStatisticsRequest()
      .withNamespace(namespace)
      .withMetricName(metricName)
      .withDimensions(dimensions.asJava)
      .withPeriod(60 * 60 * 24)  // 1 day (24 hrs)
      .withStartTime(now.minusDays(90).toDate)
      .withEndTime(now.toDate)
      .withStatistics(Statistic.Maximum)
  }

  private[metrics] def extractDataFromResult(result: GetMetricStatisticsResult): List[(DateTime, Double)] = {
    result.getDatapoints.asScala.toList.map { dp =>
      new DateTime(dp.getTimestamp) -> dp.getMaximum.toDouble
    }.sortBy(_._1.getMillis)
  }

  private def getWithRequest(request: GetMetricStatisticsRequest)(implicit executionContext: ExecutionContext): Future[List[(DateTime, Double)]] = {
    awsToScala(client.getMetricStatisticsAsync)(request).map(extractDataFromResult)
  }

  private def putWithRequest(request: PutMetricDataRequest) = {
    awsToScala(client.putMetricDataAsync)(request)
  }

  def get(metricName: String)(implicit executionContext: ExecutionContext): Attempt[Option[List[(DateTime, Double)]]] = {
    Attempt.fromFuture(getWithRequest(getRequest(metricName)).map(ds => Right(Option(ds)))){ case e =>
      Logger.warn("Failed to fetch CloudWatch data", e)
      Right(None)
    }
  }

  def put(metricName: String, maybeValue: Option[Int]): Unit = {
    maybeValue.fold {
      Logger.warn(s"Not updating CloudWatch - no value available for '$metricName'")
    }{ value =>
      putWithRequest(putRequest(metricName, value))
      Logger.debug(s"Updated CloudWatch metric '$metricName' with value '$value'")
    }
  }
}
