package config

import play.api.libs.ws.WSClient

case class AMIableConfig(prismUrl: String, wsClient: WSClient)
