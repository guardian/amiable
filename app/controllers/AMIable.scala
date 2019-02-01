package controllers

import com.gu.googleauth.AuthAction
import config.{AMIableConfig, AmiableConfigProvider}
import metrics.Charts
import models._
import play.api._
import play.api.mvc._
import prism.{Prism, PrismLogic, Recommendations}
import services.Agents
import services.notification.Notifications

import scala.concurrent.ExecutionContext

class AMIable (val controllerComponents: ControllerComponents, val amiableConfigProvider: AmiableConfigProvider, agents: Agents, notifications: Notifications, authAction: AuthAction[AnyContent])
             (implicit exec: ExecutionContext) extends BaseController {

  implicit val conf: AMIableConfig = amiableConfigProvider.conf

  def index: Action[AnyContent] = authAction.async { implicit request =>
    val ssa = SSA(stage = Some("PROD"))
    val charts = Charts.charts(
      instanceCountHistory = agents.oldProdInstanceCountHistory,
      age25thPercentileHistory = agents.amisAgePercentile25thHistory,
      age50thPercentileHistory = agents.amisAgePercentile50thHistory,
      age75thPercentileHistory = agents.amisAgePercentile75thHistory
    )
    attempt {
      for {
        instancesWithAmis <- Prism.instancesWithAmis(ssa)
        oldInstances = PrismLogic.oldInstances(instancesWithAmis)
        oldStacks = PrismLogic.stacks(oldInstances)
        agePercentiles = PrismLogic.instancesAmisAgePercentiles(instancesWithAmis)
        metrics = Metrics(oldInstances.length, instancesWithAmis.length, agePercentiles)
      } yield Ok(views.html.index(oldStacks.sorted, charts, metrics))
    }
  }

  def ami(imageId: String): Action[AnyContent] = authAction.async { implicit request =>
    attempt {
      for {
        amis <- Prism.getAMIs()
        ami <- AMI.extract(imageId, amis)
        amiWithUpgrade = Recommendations.amiWithUpgrade(agents.allAmis)(ami)
        instances <- Prism.imageUsage(ami)
        launchConfigs <- Prism.launchConfigUsage(ami)
      } yield Ok(views.html.ami(
        amiWithUpgrade,
        PrismLogic.sortInstancesByStack(instances),
        PrismLogic.sortLCsByOwner(launchConfigs)))
    }
  }

  def ssaInstanceAMIs(stackOpt: Option[String], stageOpt: Option[String], appOpt: Option[String]): Action[AnyContent] = authAction.async { implicit request =>
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
        allSSAs = ssa :: PrismLogic.instanceSSAs(instances)
        instancesCount = PrismLogic.instancesCountPerSsaPerAmi(amisWithInstances, allSSAs)
      } yield Ok(views.html.instanceAMIs(
        ssa,
        metrics,
        amisWithUpgrades.sortBy(_.creationDate.map(_.getMillis)),
        PrismLogic.sortSSAAmisByAge(amiSSAs),
        instancesCount
      ))
    }
  }

  def sendEmail: Action[AnyContent] = authAction.async {
    attempt {
      for {
        emailIds <- notifications.sendEmail()
      } yield Ok(s"Emails sent (ids):\n ${emailIds.mkString("\n")}")
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
