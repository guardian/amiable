package services

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.agent.Agent
import config.AmiableConfigProvider
import models.{AMI, SSA}
import play.api.inject.ApplicationLifecycle
import play.api.{Environment, Logger, Mode}
import prism.{Prism, PrismLogic}
import rx.lang.scala.Observable

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class Agents @Inject()(amiableConfigProvider: AmiableConfigProvider, lifecycle: ApplicationLifecycle, system: ActorSystem, environment: Environment)(implicit exec: ExecutionContext) {
  lazy implicit val conf = amiableConfigProvider.conf
  val refreshInterval = 5.minutes

  private val amisAgent: Agent[Set[AMI]] = Agent(Set.empty)
  private val ssasAgent: Agent[Set[SSA]] = Agent(Set.empty)
  private val oldProdInstanceCountAgent: Agent[Option[Int]] = Agent(None)

  def allAmis: Set[AMI] = amisAgent.get
  def allSSAs: Set[SSA] = ssasAgent.get
  def oldProdInstanceCount: Option[Int] = oldProdInstanceCountAgent.get

  if (environment.mode != Mode.Test) {
    refreshAmis()
    refreshSSAs()
    refreshOldProdInstanceCount()

    val subscription = Observable.interval(refreshInterval).subscribe { _ =>
      Logger.debug(s"Refreshing agents")
      refreshAmis()
      refreshSSAs()
      refreshOldProdInstanceCount()
    }

    lifecycle.addStopHook { () =>
      subscription.unsubscribe()
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

  def refreshOldProdInstanceCount(): Unit = {
    Prism.instancesWithAmis(SSA(stage = Some("PROD"))).fold(
      { err =>
        Logger.warn(s"Failed to update old PROD instance count ${err.logString}")
      },
      { prodInstances =>
        val oldInstances = PrismLogic.oldInstances(prodInstances)
        Logger.debug(s"Found ${oldInstances.size} PROD instances running on an out-of-date AMI")
        oldProdInstanceCountAgent.send(Some(oldInstances.size))
      }
    )
  }
}
