package config

import java.io.FileInputStream
import controllers.routes

import javax.inject.Inject
import play.api.Configuration
import play.api.http.HttpConfiguration
import play.api.libs.ws.WSClient

import scala.util.Try

case class AMIableConfig(
    prismUrl: String,
    amigoUrl: String,
    wsClient: WSClient,
    mailAddress: String,
    ownerNotificationCron: Option[String],
    overrideToAddress: Option[String],
    amiableUrl: String
)

class AmiableConfigProvider @Inject() (
    val ws: WSClient,
    val playConfig: Configuration,
    val httpConfiguration: HttpConfiguration
) {

  val amiableUrl: String = requiredString(playConfig, "host")

  val conf = AMIableConfig(
    playConfig.get[String]("prism.url"),
    playConfig.get[String]("amigo.url"),
    ws,
    playConfig.get[String]("amiable.mailClient.fromAddress"),
    playConfig.getOptional[String]("amiable.owner.notification.cron"),
    playConfig.getOptional[String](
      "amiable.owner.notification.overrideToAddress"
    ),
    amiableUrl
  )

  val stage: String = requiredString(
    playConfig,
    "stage"
  )

  /** Cloudwatch metrics are expensive. Our pre-PROD environments do not get
    * much traffic, so in order to reduce AWS spend, only create metrics in
    * PROD.
    */
  val shouldCreateCloudwatchMetrics: Boolean = stage == "PROD"

  /** If our pre-PROD environments are not creating metrics, have them read
    * PROD's so the app can be fully tested.
    * @see
    *   [[`shouldCreateCloudwatchMetrics`]]
    */
  val cloudwatchReadNamespace: String =
    if (shouldCreateCloudwatchMetrics) s"AMIable-$stage" else s"AMIable-PROD"

  val cloudwatchWriteNamespace: String = s"AMIable-$stage"
  val cloudwatchSecurityHqNamespace: String = "SecurityHQ"

  private def requiredString(config: Configuration, key: String): String = {
    config.getOptional[String](key).getOrElse {
      throw new RuntimeException(s"Missing required config property $key")
    }
  }

}
