package services

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.agent.Agent
import config.AmiableConfigProvider
import metrics.{CloudWatch, CloudWatchMetrics}
import models.{AMI, SSA}
import org.joda.time.DateTime
import play.api.inject.ApplicationLifecycle
import play.api.{Environment, Logger, Mode}
import prism.{Prism, PrismLogic}
import rx.lang.scala.Observable
import utils.Percentiles

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class Agents @Inject()(amiableConfigProvider: AmiableConfigProvider, lifecycle: ApplicationLifecycle, system: ActorSystem, environment: Environment)(implicit exec: ExecutionContext) {
  lazy implicit val conf = amiableConfigProvider.conf
  val refreshInterval = 5.minutes

  private val amisAgent: Agent[Set[AMI]] = Agent(Set.empty)
  private val ssasAgent: Agent[Set[SSA]] = Agent(Set.empty)
  private val oldProdInstanceCountAgent: Agent[Option[Int]] = Agent(None)
  private val oldProdInstanceCountHistoryAgent: Agent[List[(DateTime, Double)]] = Agent(Nil)
  private val amisAgePercentilesAgent: Agent[Option[Percentiles]] = Agent(None)

  def allAmis: Set[AMI] = amisAgent.get
  def allSSAs: Set[SSA] = ssasAgent.get
  def oldProdInstanceCount: Option[Int] = oldProdInstanceCountAgent.get
  def oldProdInstanceCountHistory: List[(DateTime, Double)] = oldProdInstanceCountHistoryAgent.get
  def amisAgePercentiles: Option[Percentiles] = amisAgePercentilesAgent.get

  if (environment.mode != Mode.Test) {
    refreshAmis()
    refreshSSAs()
    refreshInstancesInfo()
    refreshOldProdInstanceCountHistory()

    val prismDataSubscription = Observable.interval(refreshInterval).subscribe { i =>
      Logger.debug(s"Refreshing agents")
      refreshAmis()
      refreshSSAs()
      refreshInstancesInfo()
    }
    val cloudwatchDataSubscription = Observable.interval(1.hour).subscribe { i =>
      refreshOldProdInstanceCountHistory()
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
        Logger.debug(s"AMIs age percentiles: $agePercentiles")
        amisAgePercentilesAgent.send(Some(agePercentiles))
      }
    )
  }

  def refreshOldProdInstanceCountHistory(): Unit = {
    CloudWatch.get(CloudWatchMetrics.OldCount.name).fold(
      { err =>
        Logger.warn(s"Failed to update old PROD instance count ${err.logString}")
      },
      { dataOpt =>
        dataOpt.fold {
          Logger.warn(s"Failed to fetch old PROD instance count historical data")
        } { data =>
          Logger.debug(s"Found ${data.size} historical datapoints for out-of-date AMI stats")
          oldProdInstanceCountHistoryAgent.send(data)
        }
      }
    )
  }
}
