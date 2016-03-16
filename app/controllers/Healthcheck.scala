package controllers

import javax.inject.Inject

import play.api.mvc._

class Healthcheck @Inject() extends Controller {

  def healthcheck = Action {
    Ok("ok")
  }

}
