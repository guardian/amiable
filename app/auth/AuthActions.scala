package auth

import com.gu.googleauth.{GoogleAuthConfig, Actions => GoogleAuthActions}
import config.AmiableConfigProvider
import controllers.routes


trait AuthActions extends GoogleAuthActions {
  val amiableConfigProvider: AmiableConfigProvider
  override def wsClient = amiableConfigProvider.ws

  override val loginTarget           = routes.Login.startLogin()
  override val defaultRedirectTarget = routes.AMIable.index()
  override val failureRedirectTarget = routes.Login.loginError()

  override val authConfig: GoogleAuthConfig = amiableConfigProvider.googleAuthConfig
}
