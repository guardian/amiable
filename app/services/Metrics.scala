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

  def saveMetric(metricName: String, maybeValue: Option[Int]): Unit = {
    maybeValue.fold {
      Logger.warn(s"Not updating CloudWatch - no value available for '$metricName'")
    }{ value =>
      CloudWatch.put(metricName, value)
      Logger.debug(s"Updated CloudWatch metric '$metricName' with value '$value'")
    }
  }

  // only add metrics from PROD
  if (environment.mode == Mode.Prod) {

    val subscription = Observable.interval(initialDelay = 10.seconds, period = agents.refreshInterval).subscribe { _ =>
      saveMetric(CloudWatchMetrics.OldCount.name, agents.oldProdInstanceCount)
      saveMetric(CloudWatchMetrics.AmisAgePercentile25th.name, agents.amisAgePercentiles.flatMap(_.p25))
      saveMetric(CloudWatchMetrics.AmisAgePercentile50th.name, agents.amisAgePercentiles.flatMap(_.p50))
      saveMetric(CloudWatchMetrics.AmisAgePercentile75th.name, agents.amisAgePercentiles.flatMap(_.p75))
      saveMetric(CloudWatchMetrics.AmisAgePercentile90th.name, agents.amisAgePercentiles.flatMap(_.p90))
      saveMetric(CloudWatchMetrics.AmisAgePercentileHighest.name, agents.amisAgePercentiles.flatMap(_.highest))
    }

    lifecycle.addStopHook { () =>
      subscription.unsubscribe()
      Future.successful(())
    }
  }
}
