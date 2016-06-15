package config

import java.io.FileInputStream
import javax.inject.{Inject, Singleton}

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.gu.googleauth.{GoogleAuthConfig, GoogleGroupChecker, GoogleServiceAccount}
import controllers.routes
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.util.Try


case class AMIableConfig(prismUrl: String, wsClient: WSClient)

case class AuthConfig(googleAuthConfig: GoogleAuthConfig, googleGroupChecker: GoogleGroupChecker, requiredGoogleGroups: Set[String])

@Singleton
class AmiableConfigProvider @Inject()(val ws: WSClient, val playConfig: Configuration) {
  val conf = AMIableConfig(playConfig.getString("prism.url").get, ws)

  val requiredGoogleGroups = Set(requiredString(playConfig, "auth.google.2faGroupId"))

  val googleAuthConfig: GoogleAuthConfig = {
    GoogleAuthConfig(
      clientId     = requiredString(playConfig, "auth.google.clientId"),
      clientSecret = requiredString(playConfig, "auth.google.clientSecret"),
      redirectUrl  = s"${requiredString(playConfig, "host")}${routes.Login.oauth2Callback().url}",
      domain       = playConfig.getString("auth.google.apps-domain")
    )
  }

  val googleGroupChecker = {
    val twoFAUser = requiredString(playConfig, "auth.google.2faUser")
    val serviceAccountCertPath = requiredString(playConfig, "auth.google.serviceAccountCertPath")

    val credentials: GoogleCredential = {
      val jsonCertStream =
        Try(new FileInputStream(serviceAccountCertPath))
          .getOrElse(throw new RuntimeException(s"Could not load service account JSON from $serviceAccountCertPath"))
      GoogleCredential.fromStream(jsonCertStream)
    }

    val serviceAccount = GoogleServiceAccount(
      credentials.getServiceAccountId,
      credentials.getServiceAccountPrivateKey,
      twoFAUser
    )
    new GoogleGroupChecker(serviceAccount)
  }

  private def requiredString(config: Configuration, key: String): String = {
    config.getString(key).getOrElse {
      throw new RuntimeException(s"Missing required config property $key")
    }
  }
}
