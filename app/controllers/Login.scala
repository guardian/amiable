package controllers

import auth.AuthActions
import config.AmiableConfigProvider
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext


class Login (override val amiableConfigProvider: AmiableConfigProvider, override val wsClient: WSClient)
                     (implicit exec: ExecutionContext) extends Controller with AuthActions {

  val requiredGroups = amiableConfigProvider.requiredGoogleGroups
  val googleGroupChecker = amiableConfigProvider.googleGroupChecker

  def loginError = Action { request =>
    val error = request.flash.get("error")
    Ok(views.html.loginError(error))
  }

  /*
   * Redirect to Google with anti forgery token (that we keep in session storage - note that flashing is NOT secure).
   */
  def startLogin = Action.async { implicit request =>
    startGoogleLogin()
  }

  /*
   * Looks up user's identity via Google and (optionally) enforces required Google groups at login time.
   *
   * To re-check Google group membership on every page request you can use the `requireGroup` filter
   * (see `Application.scala`).
   */
  def oauth2Callback = Action.async { implicit request =>
    processOauth2Callback(requiredGroups, googleGroupChecker)
  }

  def logout = Action { implicit request =>
    Redirect(routes.Login.loggedOut()).withNewSession
  }

  def loggedOut = Action {
    Ok(views.html.loggedOut())
  }
}
