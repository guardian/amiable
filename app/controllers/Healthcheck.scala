package controllers

import play.api.Logging
import play.api.mvc._

class Healthcheck(val controllerComponents: ControllerComponents) extends BaseController with Logging {

  def healthcheck: Action[AnyContent] = Action {
    logger.info("Healthcheck was successful")
    Ok("ok")
  }

}
