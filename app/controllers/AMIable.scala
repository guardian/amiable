package controllers

import play.api._
import play.api.mvc._

class AMIable extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

}
