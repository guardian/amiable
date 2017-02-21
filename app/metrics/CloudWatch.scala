package metrics

import aws.AwsAsyncHandler.awsToScala
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.amazonaws.services.cloudwatch.model._
import models.{AMIableErrors, Attempt}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}


sealed abstract class CloudWatchMetric(val name: String)
object CloudWatchMetrics {
  case object OldCount extends CloudWatchMetric("instances-running-out-of-date-amis")
}

object CloudWatch {
  lazy val client = {
    val credentialsProvider = new AWSCredentialsProviderChain(
      new InstanceProfileCredentialsProvider(),
      new ProfileCredentialsProvider("deployTools")
    )
    val region = Option(Regions.getCurrentRegion).getOrElse(Region.getRegion(Regions.EU_WEST_1))
    val acwac = new AmazonCloudWatchAsyncClient(credentialsProvider)
    acwac.setRegion(region)
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

  private[metrics] def putRequest(metricName: String, count: Int): PutMetricDataRequest = {
    new PutMetricDataRequest()
      .withNamespace(namespace)
      .withMetricData {
        new MetricDatum()
          .withMetricName(metricName)
          .withValue(count.toDouble)
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
      .withStartTime(now.minusDays(60).toDate)
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

  private def attemptWithRequest(request: GetMetricStatisticsRequest)(implicit executionContext: ExecutionContext): Attempt[Option[List[(DateTime, Double)]]] = {
    val tmp: Future[Either[AMIableErrors, Option[List[(DateTime, Double)]]]] = getWithRequest(request).map(ds => Right(Some(ds)))
    Attempt.fromFuture(tmp){ case e =>
      Logger.warn("Failed to fetch old instance count cloudwatch data", e)
      Right(None)
    }
  }

  def get(metricName: String)(implicit executionContext: ExecutionContext): Attempt[Option[List[(DateTime, Double)]]] = {
    attemptWithRequest(getRequest(metricName))
  }

  def put(metricName: String, value: Int) = putWithRequest(putRequest(metricName, value))
}
