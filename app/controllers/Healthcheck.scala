package controllers

import play.api.mvc._

class Healthcheck(val controllerComponents: ControllerComponents) extends BaseController {

  def healthcheck: Action[AnyContent] = Action {
    Ok("ok")
  }

}
