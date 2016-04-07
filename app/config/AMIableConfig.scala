package config

import javax.inject.{Inject, Singleton}

import play.api.Configuration
import play.api.libs.ws.WSClient

case class AMIableConfig(prismUrl: String, wsClient: WSClient)

@Singleton
class AmiableConfigProvider @Inject()(ws: WSClient, playConfig: Configuration) {
  val conf = AMIableConfig(playConfig.getString("prism.url").get, ws)
}
