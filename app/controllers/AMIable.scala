package controllers

import javax.inject.Inject

import auth.AuthActions
import config.AmiableConfigProvider
import models._
import play.api._
import play.api.mvc._
import prism.{Prism, PrismLogic, Recommendations}
import services.Agents

import scala.concurrent.ExecutionContext


class AMIable @Inject()(override val amiableConfigProvider: AmiableConfigProvider, agents: Agents)
                       (implicit exec: ExecutionContext) extends Controller with AuthActions {
  implicit val conf = amiableConfigProvider.conf

  def index = AuthAction.async { implicit request =>
    val ssa = SSA(stage = Some("PROD"))
    attempt {
      for {
        instancesWithAmis <- Prism.instancesWithAmis(ssa)
        oldInstances = PrismLogic.oldInstances(instancesWithAmis)
        oldStacks = PrismLogic.stacks(oldInstances)
        agePercentiles = PrismLogic.instancesAmisAgePercentiles(instancesWithAmis)
        metrics = Metrics(oldInstances.length, instancesWithAmis.length, agePercentiles)
      } yield Ok(views.html.index(oldStacks.sorted, agents.oldProdInstanceCountHistory, metrics))
    }
  }

  def ami(imageId: String) = AuthAction.async { implicit request =>
    attempt {
      for {
        amis <- Prism.getAMIs()
        ami = amis.find(_.imageId == imageId)
        amiWithUpgrade = ami.map(Recommendations.amiWithUpgrade(agents.allAmis))
      } yield Ok(views.html.ami(amiWithUpgrade))
    }
  }

  def ssaInstanceAMIs(stackOpt: Option[String], stageOpt: Option[String], appOpt: Option[String]) = AuthAction.async { implicit request =>
    val ssa = SSA.fromParams(stackOpt, stageOpt, appOpt)
    attempt {
      for {
        instancesWithAmis <- Prism.instancesWithAmis(ssa)
        instances = instancesWithAmis.map(_._1)
        amis = instancesWithAmis.flatMap(_._2).distinct
        amisWithUpgrades = amis.map(Recommendations.amiWithUpgrade(agents.allAmis))
        amisWithInstances = PrismLogic.amiInstances(amisWithUpgrades, instances)
        oldInstances = PrismLogic.oldInstances(instancesWithAmis)
        amiSSAs = PrismLogic.amiSSAs(amisWithInstances)
        metrics = Metrics(
          oldInstancesCount = oldInstances.length,
          totalInstancesCount = instances.length,
          PrismLogic.instancesAmisAgePercentiles(instancesWithAmis)
        )
        allSSAs = PrismLogic.instanceSSAs(instances) :+ ssa
        instancesCount = PrismLogic.instancesCountPerSSA(amisWithInstances, allSSAs)
      } yield Ok(views.html.instanceAMIs(
          ssa,
          metrics,
          amisWithUpgrades.sortBy(_.creationDate.map(_.getMillis)),
          PrismLogic.sortSSAAmisByAge(amiSSAs),
          instancesCount
        ))
    }
  }

  /**
    * `Attempt` with nicely formatted error handling using the error template
    */
  private def attempt[A](action: => Attempt[Result]) = {
    Attempt(action) { err =>
      Logger.error(err.logString)
      Status(err.statusCode)(views.html.error(err))
    }
  }
}
