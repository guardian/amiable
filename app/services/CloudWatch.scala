package services

import javax.inject.{Inject, Singleton}

import aws.AwsAsyncHandler.awsToScala
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}
import play.api.inject.ApplicationLifecycle
import play.api.{Environment, Logger, Mode}
import rx.lang.scala.Observable

import scala.concurrent.Future
import scala.concurrent.duration._


@Singleton
class CloudWatch @Inject()(environment: Environment, agents: Agents, lifecycle: ApplicationLifecycle) {
  // only add metrics from PROD
  if (environment.mode == Mode.Prod) {
    val credentialsProvider = new AWSCredentialsProviderChain(
      new InstanceProfileCredentialsProvider()
      // add profile to chain if required for local dev
      // new ProfileCredentialsProvider("profile-name")
    )
    val cloudwatchClient: AmazonCloudWatchAsyncClient = new AmazonCloudWatchAsyncClient(credentialsProvider).withRegion(Regions.EU_WEST_1)

    val subscription = Observable.interval(initialDelay = 10.seconds, period = agents.refreshInterval).subscribe { _ =>
      Logger.info(s"Updating cloudwatch, ${agents.oldProdInstanceCount}")
      agents.oldProdInstanceCount.fold {
        Logger.warn("Not updating cloudwatch - no old PROD instance count available")
      }{ count =>
        val metricDataRequest = CloudWatch.putOldCountRequest(count)
        awsToScala(cloudwatchClient.putMetricDataAsync)(metricDataRequest)
        Logger.debug(s"Updated CloudWatch with out-of-date instances count: $count")
      }
    }

    lifecycle.addStopHook { () =>
      subscription.unsubscribe()
      Future.successful(())
    }
  }
}
object CloudWatch {
  def putOldCountRequest(count: Int): PutMetricDataRequest = {
    new PutMetricDataRequest()
      .withNamespace("AMIs")
      .withMetricData {
        new MetricDatum()
          .withMetricName("instances-running-out-of-date-amis")
          .withValue(count.toDouble)
          .withDimensions (
            new Dimension()
              .withName("stage")
              .withValue("PROD"),
            new Dimension()
              .withName("stack")
              .withValue("*")
          )
      }
  }
}
