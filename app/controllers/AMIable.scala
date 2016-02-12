package controllers

import config.AMIableConfig
import models.{Attempt, AMI, AMIableErrors}
import play.api._
import play.api.mvc._
import prism.PrismClient
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global


class AMIable extends Controller {

  implicit val app = play.api.Play.current
  lazy implicit val playConfig: Configuration = play.api.Play.configuration
  lazy implicit val conf = AMIableConfig(playConfig.getString("prism.url").get, WS.client)

  def index = Action {
    Ok(views.html.index())
  }

  def ami(arn: String) = Action.async { implicit request =>
    Attempt {
      for {
        ami <- PrismClient.getAMI(arn)
      } yield Ok(views.html.ami(ami))
    } { err =>
      Logger.error(err.errors.map(_.message).mkString(", "))
      Status(err.statusCode)(err.errors.map(_.friendlyMessage).mkString(", "))
    }
  }

  def amis = Action.async { implicit request =>
    Attempt {
      for {
        amis <- PrismClient.getAMIs()
      } yield Ok(views.html.amis(amis))
    } { err =>
      Logger.error(err.errors.map(_.message).mkString(", "))
      Status(err.statusCode)(err.errors.map(_.friendlyMessage).mkString(", "))
    }
  }

  def ssaInstances(stack: String, stage: String, app: String) = Action.async { implicit request =>
    Attempt {
      for {
        instances <- PrismClient.getInstances(stack, stage, app)
      } yield Ok(views.html.instances(stack, stage, app, instances))
    } { err =>
      Logger.error(err.errors.map(_.message).mkString(", "))
      Status(err.statusCode)(err.errors.map(_.friendlyMessage).mkString(", "))
    }
  }

  def ssaInstanceAMIs(stack: String, stage: String, app: String) = Action.async { implicit request =>
    Attempt {
      for {
        instances <- PrismClient.getInstances(stack, stage, app)
        amiArns = instances.flatMap(_.amiArn).distinct
        amis <- Attempt.sequence(amiArns.map(PrismClient.getAMI))
      } yield Ok(views.html.instanceAMIs(stack, stage, app, amis))
    } { err =>
      Logger.error(err.errors.map(_.message).mkString(", "))
      Status(err.statusCode)(err.errors.map(_.friendlyMessage).mkString(", "))
    }
  }
}
