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


object CloudWatch {
  val client = {
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
  val metricName = "instances-running-out-of-date-amis"

  val dimensions = List(
    new Dimension()
      .withName("stage")
      .withValue(prodStage),
    new Dimension()
      .withName("stack")
      .withValue(allStacks)
  )

  def putOldCountRequest(count: Int): PutMetricDataRequest = {
    new PutMetricDataRequest()
      .withNamespace(namespace)
      .withMetricData {
        new MetricDatum()
          .withMetricName(metricName)
          .withValue(count.toDouble)
          .withDimensions(dimensions.asJava)
      }
  }

  def getOldCountRequest: GetMetricStatisticsRequest = {
    val now = DateTime.now(DateTimeZone.UTC)
    new GetMetricStatisticsRequest()
      .withNamespace(namespace)
      .withMetricName(metricName)
      .withDimensions(dimensions.asJava)
      .withPeriod(60 * 60 * 6)  // 6 hours
      .withStartTime(now.minusDays(30).toDate)
      .withEndTime(now.toDate)
      .withStatistics(Statistic.Maximum)
  }

  def extractCountRequestData(result: GetMetricStatisticsResult): List[(DateTime, Double)] = {
    val tmp = result.getDatapoints.asScala.toList.map { dp =>
      new DateTime(dp.getTimestamp) -> dp.getMaximum.toDouble
    }.sortBy(_._1.getMillis)
    tmp.foreach { case (dt, v) => println(dt) }
    tmp
  }

  def getOldCountData(client: AmazonCloudWatchAsyncClient)(implicit executionContext: ExecutionContext): Future[List[(DateTime, Double)]] = {
    val request = getOldCountRequest
    awsToScala(client.getMetricStatisticsAsync)(getOldCountRequest).map(extractCountRequestData)
  }

  def attemptOldCountData(client: AmazonCloudWatchAsyncClient)(implicit executionContext: ExecutionContext): Attempt[Option[List[(DateTime, Double)]]] = {
    val tmp: Future[Either[AMIableErrors, Option[List[(DateTime, Double)]]]] = getOldCountData(client).map(ds => Right(Some(ds)))
    Attempt.fromFuture(tmp){ case e =>
      Logger.warn("Failed to fetch old instance count cloudwatch data", e)
      Right(None)
    }
  }
}
