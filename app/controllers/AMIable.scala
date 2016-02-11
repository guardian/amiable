package controllers

import config.AMIableConfig
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
    PrismClient.getAMI(arn).map {
      case Right(ami) => Ok(views.html.ami(ami))
      case Left(err) =>
        Logger.error(err.errors.map(_.message).mkString(", "))
        Status(err.statusCode)(err.errors.map(_.friendlyMessage).mkString(", "))
    }
  }

  def amis = Action.async { implicit request =>
    PrismClient.getAMIs().map {
      case Right(amis) => Ok(views.html.amis(amis))
      case Left(err) =>
        Logger.error(err.errors.map(_.message).mkString(", "))
        Status(err.statusCode)(err.errors.map(_.friendlyMessage).mkString(", "))
    }
  }
}
