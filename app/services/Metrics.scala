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
      Logger.info(s"Updating cloudwatch, ${agents.oldProdInstanceCount}")
      agents.oldProdInstanceCount.fold {
        Logger.warn("Not updating cloudwatch - no old PROD instance count available")
      }{ count =>
        CloudWatch.put(CloudWatchMetrics.OldCount.name, count)
        Logger.debug(s"Updated CloudWatch with out-of-date instances count: $count")
      }
    }

    lifecycle.addStopHook { () =>
      subscription.unsubscribe()
      Future.successful(())
    }
  }
}
