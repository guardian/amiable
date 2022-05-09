package services

import metrics.{CloudWatch, CloudWatchMetrics}
import play.api.inject.ApplicationLifecycle
import play.api.{Environment, Mode}
import rx.lang.scala.Observable

import scala.concurrent.Future
import scala.concurrent.duration._

class Metrics(cloudWatch: CloudWatch, shouldCreateMetrics: Boolean, namespace: String, securityHqNamespace: String, agents: Agents, lifecycle: ApplicationLifecycle) {
  if (shouldCreateMetrics) {

    val subscription = Observable.interval(initialDelay = 10.seconds, period = 6.hours).subscribe { _ =>
      cloudWatch.put(namespace, CloudWatchMetrics.OldCount.name, agents.oldProdInstanceCount)
      cloudWatch.put(namespace, CloudWatchMetrics.AmisAgePercentile25th.name, agents.amisAgePercentiles.flatMap(_.p25))
      cloudWatch.put(namespace, CloudWatchMetrics.AmisAgePercentile50th.name, agents.amisAgePercentiles.flatMap(_.p50))
      cloudWatch.put(namespace, CloudWatchMetrics.AmisAgePercentile75th.name, agents.amisAgePercentiles.flatMap(_.p75))
      cloudWatch.put(namespace, CloudWatchMetrics.AmisAgePercentile90th.name, agents.amisAgePercentiles.flatMap(_.p90))
      cloudWatch.put(namespace, CloudWatchMetrics.AmisAgePercentileHighest.name, agents.amisAgePercentiles.flatMap(_.highest))

      // These metrics are used by a Grafana dashboard which graphs SecurityHQ related metrics
      // TODO use the same namespace as other metrics, and filter in Grafana instead?
      cloudWatch.put(securityHqNamespace, CloudWatchMetrics.OldCountByAccount.name, agents.oldInstanceCountByAccount)
    }

    lifecycle.addStopHook { () =>
      subscription.unsubscribe()
      Future.successful(())
    }
  }
}
