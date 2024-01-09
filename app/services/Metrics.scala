package services

import metrics.{CloudWatch, CloudWatchMetrics}
import org.apache.pekko.actor.ActorSystem
import play.api.inject.ApplicationLifecycle
import play.api.{Environment, Mode}
import play.api.libs.concurrent.Pekko

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.*

class Metrics(
    cloudWatch: CloudWatch,
    shouldCreateMetrics: Boolean,
    namespace: String,
    securityHqNamespace: String,
    agents: Agents,
    lifecycle: ApplicationLifecycle,
    system: ActorSystem
) {
  implicit val ec: ExecutionContext = system.dispatcher

  if (shouldCreateMetrics) {

    val subscription = system.scheduler.scheduleAtFixedRate(
      initialDelay = 10.seconds,
      interval = 6.hours
    ) { () =>
      {
        cloudWatch.put(
          namespace,
          CloudWatchMetrics.OldCount.name,
          agents.oldProdInstanceCount
        )
        cloudWatch.put(
          namespace,
          CloudWatchMetrics.AmisAgePercentile25th.name,
          agents.amisAgePercentiles.flatMap(_.p25)
        )
        cloudWatch.put(
          namespace,
          CloudWatchMetrics.AmisAgePercentile50th.name,
          agents.amisAgePercentiles.flatMap(_.p50)
        )
        cloudWatch.put(
          namespace,
          CloudWatchMetrics.AmisAgePercentile75th.name,
          agents.amisAgePercentiles.flatMap(_.p75)
        )
        cloudWatch.put(
          namespace,
          CloudWatchMetrics.AmisAgePercentile90th.name,
          agents.amisAgePercentiles.flatMap(_.p90)
        )
        cloudWatch.put(
          namespace,
          CloudWatchMetrics.AmisAgePercentileHighest.name,
          agents.amisAgePercentiles.flatMap(_.highest)
        )

        // These metrics are used by a Grafana dashboard which graphs SecurityHQ related metrics
        // TODO use the same namespace as other metrics, and filter in Grafana instead?
        cloudWatch.put(
          securityHqNamespace,
          CloudWatchMetrics.OldCountByAccount.name,
          agents.oldInstanceCountByAccount
        )
      }
    }

    lifecycle.addStopHook { () =>
      subscription.cancel()
      Future.successful(())
    }
  }
}
