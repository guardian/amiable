package controllers

import config.AMIableConfig
import models.Attempt
import play.api._
import play.api.libs.ws._
import play.api.mvc._
import prism.Prism

import scala.concurrent.ExecutionContext.Implicits.global


class AMIable extends Controller {

  implicit val app = play.api.Play.current
  lazy implicit val playConfig: Configuration = play.api.Play.configuration
  lazy implicit val conf = AMIableConfig(playConfig.getString("prism.url").get, WS.client)

  def index = Action {
    Ok(views.html.index())
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

  def ssaInstances(stack: Option[String], stage: Option[String], app: Option[String]) = Action.async { implicit request =>
    attempt {
      for {
        instances <- Prism.getInstances(stack, stage, app)
      } yield Ok(views.html.instances(stack, stage, app, instances))
    }
  }

  def ssaInstanceAMIs(stack: Option[String], stage: Option[String], app: Option[String]) = Action.async { implicit request =>
    attempt {
      for {
        instances <- Prism.getInstances(stack, stage, app)
        amiArns = instances.flatMap(_.amiArn).distinct
        amis <- Attempt.sequence(amiArns.map(Prism.getAMI))
      } yield Ok(views.html.instanceAMIs(stack, stage, app, amis))
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
