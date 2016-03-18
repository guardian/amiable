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

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._


@Singleton
class Agents @Inject()(amiableConfigProvider: AmiableConfigProvider, lifecycle: ApplicationLifecycle, system: ActorSystem, environment: Environment)(implicit exec: ExecutionContext) {
  lazy implicit val conf = amiableConfigProvider.conf

  private val amisAgent: Agent[Set[AMI]] = Agent(Set.empty)
  private val ssasAgent: Agent[Set[SSA]] = Agent(Set.empty)

  def allAmis: Set[AMI] = amisAgent.get
  def allSSAs: Set[SSA] = ssasAgent.get

  if (environment.mode != Mode.Test) {
    refreshAmis()
    refreshSSAs()

    val subscription = Observable.interval(5.minutes).subscribe { _ =>
      Logger.debug(s"Refreshing agents")
      refreshAmis()
      refreshSSAs()
    }

    lifecycle.addStopHook { () =>
      subscription.unsubscribe()
      Future.successful(())
    }
  }

  def refreshAmis() = {
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

  def refreshSSAs() = {
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
}
