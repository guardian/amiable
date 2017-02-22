package services

import javax.inject.{Inject, Singleton}

import metrics.{CloudWatch, CloudWatchMetrics}
import play.api.inject.ApplicationLifecycle
import play.api.{Environment, Logger, Mode}
import rx.lang.scala.Observable

import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class Metrics @Inject()(environment: Environment, agents: Agents, lifecycle: ApplicationLifecycle) {

  // only add metrics from PROD
  if (environment.mode == Mode.Prod) {

    val subscription = Observable.interval(initialDelay = 10.seconds, period = agents.refreshInterval).subscribe { _ =>
      CloudWatch.put(CloudWatchMetrics.OldCount.name, agents.oldProdInstanceCount)
      CloudWatch.put(CloudWatchMetrics.AmisAgePercentile25th.name, agents.amisAgePercentiles.flatMap(_.p25))
      CloudWatch.put(CloudWatchMetrics.AmisAgePercentile50th.name, agents.amisAgePercentiles.flatMap(_.p50))
      CloudWatch.put(CloudWatchMetrics.AmisAgePercentile75th.name, agents.amisAgePercentiles.flatMap(_.p75))
      CloudWatch.put(CloudWatchMetrics.AmisAgePercentile90th.name, agents.amisAgePercentiles.flatMap(_.p90))
      CloudWatch.put(CloudWatchMetrics.AmisAgePercentileHighest.name, agents.amisAgePercentiles.flatMap(_.highest))
    }

    lifecycle.addStopHook { () =>
      subscription.unsubscribe()
      Future.successful(())
    }
  }
}
