package services

import akka.actor.ActorSystem
import akka.agent.Agent
import config.AmiableConfigProvider
import javax.inject.Inject
import metrics.{CloudWatch, CloudWatchMetrics}
import models._
import org.joda.time.DateTime
import play.api.inject.ApplicationLifecycle
import play.api.{Environment, Logger, Mode}
import prism.{Prism, PrismLogic}
import rx.lang.scala.Observable
import utils.Percentiles

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

case class OldInstanceAccountHistory(date: DateTime, accountName: String, count: Int)

class Agents @Inject() (amiableConfigProvider: AmiableConfigProvider, lifecycle: ApplicationLifecycle, system: ActorSystem, environment: Environment, cloudWatch: CloudWatch)(implicit exec: ExecutionContext) {

  lazy implicit val conf = amiableConfigProvider.conf
  val refreshInterval = 5.minutes

  private val amisAgent: Agent[Set[AMI]] = Agent(Set.empty)
  private val ssasAgent: Agent[Set[SSAA]] = Agent(Set.empty)
  private val oldProdInstanceCountAgent: Agent[Option[Int]] = Agent(None)
  private val oldProdInstanceCountHistoryAgent: Agent[List[(DateTime, Double)]] = Agent(Nil)
  private val amisAgePercentilesAgent: Agent[Option[Percentiles]] = Agent(None)
  private val amisAgePercentile25thHistoryAgent: Agent[List[(DateTime, Double)]] = Agent(Nil)
  private val amisAgePercentile50thHistoryAgent: Agent[List[(DateTime, Double)]] = Agent(Nil)
  private val amisAgePercentile75thHistoryAgent: Agent[List[(DateTime, Double)]] = Agent(Nil)
  private val oldInstanceCountByAccountAgent: Agent[List[OldInstanceAccountHistory]] = Agent(Nil)

  def allAmis: Set[AMI] = amisAgent.get
  def allSSAs: Set[SSAA] = ssasAgent.get
  def oldProdInstanceCount: Option[Int] = oldProdInstanceCountAgent.get
  def oldProdInstanceCountHistory: List[(DateTime, Double)] = oldProdInstanceCountHistoryAgent.get
  def amisAgePercentiles: Option[Percentiles] = amisAgePercentilesAgent.get
  def amisAgePercentile25thHistory: List[(DateTime, Double)] = amisAgePercentile25thHistoryAgent.get
  def amisAgePercentile50thHistory: List[(DateTime, Double)] = amisAgePercentile50thHistoryAgent.get
  def amisAgePercentile75thHistory: List[(DateTime, Double)] = amisAgePercentile75thHistoryAgent.get
  def oldInstanceCountByAccount: List[OldInstanceAccountHistory] = oldInstanceCountByAccountAgent.get

  if (environment.mode != Mode.Test) {
    refreshAmis()
    refreshSSAs()
    refreshInstancesInfo()
    refreshHistory()

    val prismDataSubscription = Observable.interval(refreshInterval).subscribe { i =>
      Logger.debug(s"Refreshing agents")
      refreshAmis()
      refreshSSAs()
      refreshInstancesInfo()
    }

    val cloudwatchDataSubscription = Observable.interval(1.hour).subscribe { i =>
      refreshHistory()
    }

    lifecycle.addStopHook { () =>
      prismDataSubscription.unsubscribe()
      cloudwatchDataSubscription.unsubscribe()
      Future.successful(())
    }
  }

  def refreshAmis(): Unit = {
    Prism.getAMIs().fold(
      { err =>
        Logger.warn(s"Failed to update AMIs ${err.logString}")
      },
      { amis =>
        Logger.debug(s"Loaded ${amis.size} AMIs")
        amisAgent.send(amis.toSet)
      }
    )
  }

  def refreshSSAs(): Unit = {
    Prism.getInstances(SSAA()).map(PrismLogic.instanceSSAAs).fold(
      { err =>
        Logger.warn(s"Failed to update SSAs ${err.logString}")
      },
      { ssaas =>
        Logger.debug(s"Loaded ${ssaas.size} SSAA combinations")
        ssasAgent.send(ssaas.toSet)
      }
    )
  }

  def refreshProdInstancesInfo(instancesWithAmis: List[(Instance, Option[AMI])]): Unit = {
    val prodInstancesWithAmis = instancesWithAmis.filter(_._1.stage.contains("PROD"))

    val oldProdInstances = PrismLogic.oldInstances(prodInstancesWithAmis)
    Logger.debug(s"Found ${oldProdInstances.size} PROD instances running on an out-of-date AMI")
    oldProdInstanceCountAgent.send(Some(oldProdInstances.size))

    val prodAgePercentiles = PrismLogic.instancesAmisAgePercentiles(prodInstancesWithAmis)
    Logger.debug(s"Found AMIs age percentiles (p25: ${prodAgePercentiles.p25}, p50: ${prodAgePercentiles.p50}, p75: ${prodAgePercentiles.p75})")
    amisAgePercentilesAgent.send(Some(prodAgePercentiles))
  }

  def refreshOldInstanceCountInfo(instancesWithAmis: List[(Instance, Option[AMI])]): Unit = {
    for {
      accounts <- Prism.getAccounts
    } yield {
      val now = DateTime.now
      val amisForAccount = instancesWithAmis.groupBy(_._1.meta.origin.accountName.getOrElse("unknown-account"))

      val oldInstanceCountsByAccount = accounts.map( account => {
          OldInstanceAccountHistory(now, account.accountName, PrismLogic.oldInstances(amisForAccount.getOrElse(account.accountName, List())).length)
      })

      oldInstanceCountByAccountAgent.send(oldInstanceCountsByAccount)
    }
  }

  def refreshInstancesInfo(): Unit = {
    Prism.instancesWithAmis(SSAA()).fold(
      { err =>
        Logger.warn(s"Failed to update old instance count ${err.logString}")
      },
      { instancesWithAmis =>
        refreshProdInstancesInfo(instancesWithAmis)
        refreshOldInstanceCountInfo(instancesWithAmis)
      }
    )
  }

  def refreshHistory(): Unit = {
    refreshHistory(oldProdInstanceCountHistoryAgent, CloudWatchMetrics.OldCount.name)
    refreshHistory(amisAgePercentile25thHistoryAgent, CloudWatchMetrics.AmisAgePercentile25th.name)
    refreshHistory(amisAgePercentile50thHistoryAgent, CloudWatchMetrics.AmisAgePercentile50th.name)
    refreshHistory(amisAgePercentile75thHistoryAgent, CloudWatchMetrics.AmisAgePercentile75th.name)
  }

  private def refreshHistory(agent: Agent[List[(DateTime, Double)]], metricName:String): Unit = {
    cloudWatch.get(metricName).fold(
      { err =>
        Logger.warn(s"Failed to update historical data for metric '$metricName': ${err.logString}")
      },
      { dataOpt =>
        dataOpt.fold {
          Logger.warn(s"Failed to fetch historical data for metric '$metricName'")
        } { data =>
          Logger.debug(s"Found ${data.size} historical datapoints for metric '$metricName'")
          agent.send(data)
        }
      }
    )
  }
}
