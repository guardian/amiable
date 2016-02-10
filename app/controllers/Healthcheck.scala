package controllers

import play.api.mvc._

class Healthcheck extends Controller {

  def healthcheck = Action {
    Ok("ok")
  }

}
