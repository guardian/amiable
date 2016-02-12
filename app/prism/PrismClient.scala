package prism

import java.net.URLEncoder

import config.AMIableConfig
import logic.JsonUtils
import models._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}

object PrismClient {
  def getAMI(arn : String)(implicit config: AMIableConfig, ec: ExecutionContext): Future[Attempt[AMI]] = {
    config.wsClient.url(amiUrl(arn, config.prismUrl)).get().map { response =>
      for {
        json <- amiResponseJson(response).right
        ami <- extractAMI(json).right
      } yield ami
    }
  }

  def getAMIs()(implicit config: AMIableConfig, ec: ExecutionContext): Future[Attempt[List[AMI]]] = {
    config.wsClient.url(amisUrl(config.prismUrl)).get().map { response =>
      for {
        jsons <- amisResponseJson(response).right
        amis <- AMIableErrors.flip(jsons.map(extractAMI)).right
      } yield amis
    }
  }

  def getInstances(stack: String, stage: String, app: String)(implicit config: AMIableConfig, ec: ExecutionContext): Future[Attempt[List[Instance]]] = {
    config.wsClient.url(amisUrl(config.prismUrl)).get().map { response =>
      for {
        jsons <- instancesResponseJson(response).right
        instances <- AMIableErrors.flip(jsons.map(extractInstance)).right
      } yield instances
    }
  }

  private[prism] def amiResponseJson(response: WSResponse): Attempt[JsValue] = {
    (response.json \ "data").toEither
      .left.map { valErr =>
        Logger.warn(valErr.message)
        AMIableErrors(AMIableError(valErr.message, "Could not parse AMI response JSON", 500))
      }
  }

  private[prism] def amisResponseJson(response: WSResponse): Attempt[List[JsValue]] = {
    JsonUtils.jsResultToAttempt("Could not get AMI from response JSON"){
      (response.json \ "data" \ "images").validate[List[JsValue]]
    }
  }

  private[prism] def extractAMI(json: JsValue): Attempt[AMI] = {
    JsonUtils.extractToAttempt[AMI]("Could not get AMI from response JSON") {
      json.validate[AMI]
    }
  }

  private[prism] def instancesResponseJson(response: WSResponse): Attempt[List[JsValue]] = {
    JsonUtils.jsResultToAttempt("Could not get AMI from response JSON"){
      (response.json \ "data" \ "images").validate[List[JsValue]]
    }
  }

  private[prism] def extractInstance(json: JsValue): Attempt[Instance] = {
    JsonUtils.extractToAttempt[Instance]("Could not get Instance from response JSON") {
      json.validate[Instance]
    }
  }

  private[prism] def amiUrl(arn: String, prismUrl: String): String = {
    val encodedArn = URLEncoder.encode(arn, "UTF-8")
    s"$prismUrl/images/$encodedArn"
  }

  private[prism] def amisUrl(prismUrl: String): String = {
    s"$prismUrl/images"
  }

  private[prism] def instancesUrl(stack: String, stage: String, app: String, prismUrl: String) = {
    s"$prismUrl?stack=$stack&stage=$stage&app=$app"
  }
}
