package services

import metrics.{CloudWatch, CloudWatchMetrics}
import play.api.inject.ApplicationLifecycle
import play.api.{Environment, Mode}
import rx.lang.scala.Observable

import scala.concurrent.Future
import scala.concurrent.duration._

class Metrics(cloudWatch: CloudWatch, environment: Environment, agents: Agents, lifecycle: ApplicationLifecycle) {

  // only add metrics from PROD
  if (environment.mode == Mode.Prod) {

    val subscription = Observable.interval(initialDelay = 10.seconds, period = agents.refreshInterval).subscribe { _ =>
      cloudWatch.put(CloudWatchMetrics.OldCount.name, agents.oldProdInstanceCount)
      cloudWatch.put(CloudWatchMetrics.AmisAgePercentile25th.name, agents.amisAgePercentiles.flatMap(_.p25))
      cloudWatch.put(CloudWatchMetrics.AmisAgePercentile50th.name, agents.amisAgePercentiles.flatMap(_.p50))
      cloudWatch.put(CloudWatchMetrics.AmisAgePercentile75th.name, agents.amisAgePercentiles.flatMap(_.p75))
      cloudWatch.put(CloudWatchMetrics.AmisAgePercentile90th.name, agents.amisAgePercentiles.flatMap(_.p90))
      cloudWatch.put(CloudWatchMetrics.AmisAgePercentileHighest.name, agents.amisAgePercentiles.flatMap(_.highest))
      cloudWatch.put(CloudWatchMetrics.OldCountByAccount.name, agents.oldInstanceCountByAccount)
    }

    lifecycle.addStopHook { () =>
      subscription.unsubscribe()
      Future.successful(())
    }
  }
}
