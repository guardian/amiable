package controllers

import config.AMIableConfig
import models.{SSA, Attempt}
import play.api._
import play.api.libs.ws._
import play.api.mvc._
import prism.Prism
import prism.PrismLogic

import scala.concurrent.ExecutionContext.Implicits.global


class AMIable extends Controller {

  implicit val app = play.api.Play.current
  lazy implicit val playConfig: Configuration = play.api.Play.configuration
  lazy implicit val conf = AMIableConfig(playConfig.getString("prism.url").get, WS.client)

  def index = Action.async { implicit request =>
    attempt {
      for {
        prodInstances <- Prism.instancesWithAmis(SSA(stage = Some("PROD")))
        oldInstances = PrismLogic.oldInstances(prodInstances)
        oldStacks = PrismLogic.stacks(oldInstances)
      } yield Ok(views.html.index(oldInstances, oldStacks))
    }
  }

  def ami(arn: String) = Action.async { implicit request =>
    attempt {
      for {
        ami <- Prism.getAMI(arn)
      } yield Ok(views.html.ami(ami))
    }
  }

  def amis = Action.async { implicit request =>
    attempt {
      for {
        amis <- Prism.getAMIs()
      } yield Ok(views.html.amis(amis))
    }
  }

  def ssaInstances(stackOpt: Option[String], stageOpt: Option[String], appOpt: Option[String]) = Action.async { implicit request =>
    val ssa = SSA.fromParams(stackOpt, stageOpt, appOpt)
    attempt {
      for {
        instances <- Prism.getInstances(ssa)
      } yield Ok(views.html.instances(ssa.stack, ssa.stage, ssa.app, instances))
    }
  }

  def ssaInstanceAMIs(stackOpt: Option[String], stageOpt: Option[String], appOpt: Option[String]) = Action.async { implicit request =>
    val ssa = SSA.fromParams(stackOpt, stageOpt, appOpt)
    attempt {
      for {
        instances <- Prism.getInstances(ssa)
        amiArns = instances.flatMap(_.amiArn).distinct
        amis <- Attempt.successfulAttempts(amiArns.map(Prism.getAMI))
        amisWithInstances = PrismLogic.amiInstances(amis, instances)
        amiSSAs = PrismLogic.amiSSAs(amisWithInstances)
      } yield {
        Ok(views.html.instanceAMIs(ssa.stack, ssa.stage, ssa.app, PrismLogic.sortSSAAmisByAge(amiSSAs)))
      }
    }
  }

  /**
    * `Attempt` with nicely formatted error handling using the error template
    */
  private def attempt[A](action: => Attempt[Result]) = {
    Attempt(action) { err =>
      Logger.error(err.errors.map(_.message).mkString(", "))
      Status(err.statusCode)(views.html.error(err))
    }
  }
}
