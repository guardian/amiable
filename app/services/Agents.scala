package services

import org.apache.pekko.actor.ActorSystem
import config.{AMIableConfig, AmiableConfigProvider}

import javax.inject.Inject
import metrics.{CloudWatch, CloudWatchMetrics}
import models.*
import org.joda.time.DateTime
import play.api.inject.ApplicationLifecycle
import play.api.{Environment, Logging, Mode}
import prism.{Prism, PrismLogic}
import utils.Percentiles

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

case class OldInstanceAccountHistory(
    date: DateTime,
    accountName: String,
    count: Int
)

class Agents @Inject() (
    amiableConfigProvider: AmiableConfigProvider,
    lifecycle: ApplicationLifecycle,
    system: ActorSystem,
    environment: Environment,
    cloudWatch: CloudWatch
)(implicit exec: ExecutionContext)
    extends Logging {

  lazy implicit val conf: AMIableConfig = amiableConfigProvider.conf
  val refreshInterval = 5.minutes

  private val amisAgent: AtomicReference[Set[AMI]] = AtomicReference(Set.empty)
  private val ssasAgent: AtomicReference[Set[SSAA]] = AtomicReference(Set.empty)
  private val oldProdInstanceCountAgent: AtomicReference[Option[Int]] =
    AtomicReference(None)
  private val oldProdInstanceCountHistoryAgent
      : AtomicReference[List[(DateTime, Double)]] = AtomicReference(Nil)
  private val amisAgePercentilesAgent: AtomicReference[Option[Percentiles]] =
    AtomicReference(None)
  private val amisAgePercentile25thHistoryAgent
      : AtomicReference[List[(DateTime, Double)]] = AtomicReference(Nil)
  private val amisAgePercentile50thHistoryAgent
      : AtomicReference[List[(DateTime, Double)]] = AtomicReference(Nil)
  private val amisAgePercentile75thHistoryAgent
      : AtomicReference[List[(DateTime, Double)]] = AtomicReference(Nil)
  private val oldInstanceCountByAccountAgent
      : AtomicReference[List[OldInstanceAccountHistory]] = AtomicReference(Nil)

  def allAmis: Set[AMI] = amisAgent.get
  def allSSAs: Set[SSAA] = ssasAgent.get
  def oldProdInstanceCount: Option[Int] = oldProdInstanceCountAgent.get
  def oldProdInstanceCountHistory: List[(DateTime, Double)] =
    oldProdInstanceCountHistoryAgent.get
  def amisAgePercentiles: Option[Percentiles] = amisAgePercentilesAgent.get
  def amisAgePercentile25thHistory: List[(DateTime, Double)] =
    amisAgePercentile25thHistoryAgent.get
  def amisAgePercentile50thHistory: List[(DateTime, Double)] =
    amisAgePercentile50thHistoryAgent.get
  def amisAgePercentile75thHistory: List[(DateTime, Double)] =
    amisAgePercentile75thHistoryAgent.get
  def oldInstanceCountByAccount: List[OldInstanceAccountHistory] =
    oldInstanceCountByAccountAgent.get

  if (environment.mode != Mode.Test) {
    refreshAmis()
    refreshSSAs()
    refreshInstancesInfo()
    refreshHistory()

    val prismDataSubscription = system.scheduler.scheduleAtFixedRate(
      initialDelay = 0.seconds,
      interval = refreshInterval
    ) { () =>
      {
        logger.debug(s"Refreshing agents")
        refreshAmis()
        refreshSSAs()
        refreshInstancesInfo()
      }
    }(exec)

    val cloudwatchDataSubscription = system.scheduler.scheduleAtFixedRate(
      initialDelay = 0.seconds,
      interval = 1.hour
    ) { () =>
      refreshHistory()
    }(exec)

    lifecycle.addStopHook { () =>
      prismDataSubscription.cancel()
      cloudwatchDataSubscription.cancel()
      Future.successful(())
    }
  }

  def refreshAmis(): Unit = {
    Prism
      .getAMIs()
      .fold(
        { err =>
          logger.warn(s"Failed to update AMIs ${err.logString}")
        },
        { amis =>
          logger.debug(s"Loaded ${amis.size} AMIs")
          amisAgent.set(amis.toSet)
        }
      )
  }

  def refreshSSAs(): Unit = {
    Prism
      .getInstances(SSAA())
      .map(PrismLogic.instanceSSAAs)
      .fold(
        { err =>
          logger.warn(s"Failed to update SSAs ${err.logString}")
        },
        { ssaas =>
          logger.debug(s"Loaded ${ssaas.size} SSAA combinations")
          ssasAgent.set(ssaas.toSet)
        }
      )
  }

  def refreshProdInstancesInfo(
      instancesWithAmis: List[(Instance, Option[AMI])]
  ): Unit = {
    val prodInstancesWithAmis =
      instancesWithAmis.filter(_._1.stage.contains("PROD"))

    val oldProdInstances = PrismLogic.oldInstances(prodInstancesWithAmis)
    logger.debug(
      s"Found ${oldProdInstances.size} PROD instances running on an out-of-date AMI"
    )
    oldProdInstanceCountAgent.set(Some(oldProdInstances.size))

    val prodAgePercentiles =
      PrismLogic.instancesAmisAgePercentiles(prodInstancesWithAmis)
    logger.debug(
      s"Found AMIs age percentiles (p25: ${prodAgePercentiles.p25}, p50: ${prodAgePercentiles.p50}, p75: ${prodAgePercentiles.p75})"
    )
    amisAgePercentilesAgent.set(Some(prodAgePercentiles))
  }

  def refreshOldInstanceCountInfo(
      instancesWithAmis: List[(Instance, Option[AMI])]
  ): Unit = {
    for {
      accounts <- Prism.getAccounts
    } yield {
      val now = DateTime.now
      val oldInstancesForAccount = PrismLogic
        .oldInstances(instancesWithAmis)
        .groupBy(_.meta.origin.accountName.getOrElse("unknown-account"))

      val oldInstanceCountsByAccount = accounts.map(account => {
        val numberOfOldInstancesForAccount =
          oldInstancesForAccount.getOrElse(account.accountName, List()).length
        OldInstanceAccountHistory(
          now,
          account.accountName,
          numberOfOldInstancesForAccount
        )
      })

      oldInstanceCountByAccountAgent.set(oldInstanceCountsByAccount)
    }
  }

  def refreshInstancesInfo(): Unit = {
    Prism
      .instancesWithAmis(SSAA())
      .fold(
        { err =>
          logger.warn(s"Failed to update old instance count ${err.logString}")
        },
        { instancesWithAmis =>
          refreshProdInstancesInfo(instancesWithAmis)
          refreshOldInstanceCountInfo(instancesWithAmis)
        }
      )
  }

  def refreshHistory(): Unit = {
    refreshHistory(
      oldProdInstanceCountHistoryAgent,
      CloudWatchMetrics.OldCount.name
    )
    refreshHistory(
      amisAgePercentile25thHistoryAgent,
      CloudWatchMetrics.AmisAgePercentile25th.name
    )
    refreshHistory(
      amisAgePercentile50thHistoryAgent,
      CloudWatchMetrics.AmisAgePercentile50th.name
    )
    refreshHistory(
      amisAgePercentile75thHistoryAgent,
      CloudWatchMetrics.AmisAgePercentile75th.name
    )
  }

  private def refreshHistory(
      agent: AtomicReference[List[(DateTime, Double)]],
      metricName: String
  ): Unit = {
    cloudWatch
      .get(amiableConfigProvider.cloudwatchReadNamespace, metricName)
      .fold(
        { err =>
          logger.warn(
            s"Failed to update historical data for metric '$metricName': ${err.logString}"
          )
        },
        { dataOpt =>
          dataOpt.fold {
            logger
              .warn(s"Failed to fetch historical data for metric '$metricName'")
          } { data =>
            logger.debug(
              s"Found ${data.size} historical datapoints for metric '$metricName'"
            )
            agent.set(data)
          }
        }
      )
  }
}
