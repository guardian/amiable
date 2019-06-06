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


class Agents @Inject() (amiableConfigProvider: AmiableConfigProvider, lifecycle: ApplicationLifecycle, system: ActorSystem, environment: Environment)(implicit exec: ExecutionContext) {

  lazy implicit val conf = amiableConfigProvider.conf
  val refreshInterval = 5.minutes

  private val amisAgent: Agent[Set[AMI]] = Agent(Set.empty)
  private val ssasAgent: Agent[Set[SSA]] = Agent(Set.empty)
  private val oldProdInstanceCountAgent: Agent[Option[Int]] = Agent(None)
  private val oldProdInstanceCountHistoryAgent: Agent[List[(DateTime, Double)]] = Agent(Nil)
  private val amisAgePercentilesAgent: Agent[Option[Percentiles]] = Agent(None)
  private val amisAgePercentile25thHistoryAgent: Agent[List[(DateTime, Double)]] = Agent(Nil)
  private val amisAgePercentile50thHistoryAgent: Agent[List[(DateTime, Double)]] = Agent(Nil)
  private val amisAgePercentile75thHistoryAgent: Agent[List[(DateTime, Double)]] = Agent(Nil)

  def allAmis: Set[AMI] = amisAgent.get
  def allSSAs: Set[SSA] = ssasAgent.get
  def oldProdInstanceCount: Option[Int] = oldProdInstanceCountAgent.get
  def oldProdInstanceCountHistory: List[(DateTime, Double)] = oldProdInstanceCountHistoryAgent.get
  def amisAgePercentiles: Option[Percentiles] = amisAgePercentilesAgent.get
  def amisAgePercentile25thHistory: List[(DateTime, Double)] = amisAgePercentile25thHistoryAgent.get
  def amisAgePercentile50thHistory: List[(DateTime, Double)] = amisAgePercentile50thHistoryAgent.get
  def amisAgePercentile75thHistory: List[(DateTime, Double)] = amisAgePercentile75thHistoryAgent.get

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
    Prism.getInstances(SSA()).map(PrismLogic.instanceSSAs).fold(
      { err =>
        Logger.warn(s"Failed to update SSAs ${err.logString}")
      },
      { ssas =>
        Logger.debug(s"Loaded ${ssas.size} SSA combinations")
        ssasAgent.send(ssas.toSet)
      }
    )
  }

  def refreshInstancesInfo(): Unit = {
    Prism.instancesWithAmis(SSA(stage = Some("PROD"))).fold(
      { err =>
        Logger.warn(s"Failed to update old PROD instance count ${err.logString}")
      },
      { instancesWithAmis =>
        val oldInstances = PrismLogic.oldInstances(instancesWithAmis)
        Logger.debug(s"Found ${oldInstances.size} PROD instances running on an out-of-date AMI")
        oldProdInstanceCountAgent.send(Some(oldInstances.size))

        val agePercentiles = PrismLogic.instancesAmisAgePercentiles(instancesWithAmis)
        Logger.debug(s"Found AMIs age percentiles (p25: ${agePercentiles.p25}, p50: ${agePercentiles.p50}, p75: ${agePercentiles.p75})")
        amisAgePercentilesAgent.send(Some(agePercentiles))
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
    CloudWatch.get(metricName).fold(
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
