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
  def getAMI(arn : String)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[AMI] = {
    for {
      response <- Attempt.Async.Right(config.wsClient.url(amiUrl(arn, config.prismUrl)).get())
      json <- amiResponseJson(response)
      ami <- extractAMI(json)
    } yield ami
  }

  def getAMIs()(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[AMI]] = {
    for {
      response <- Attempt.Async.Right(config.wsClient.url(amisUrl(config.prismUrl)).get())
      jsons <- amisResponseJson(response)
      amis <- Attempt.sequence(jsons.map(extractAMI))
    } yield amis
  }

  def getInstances(stack: String, stage: String, app: String)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[Instance]] = {
    for {
      response <- handleWsError(config.wsClient.url(instancesUrl(stack, stage, app, config.prismUrl)).get())
      jsons <- instancesResponseJson(response)
      instances <- Attempt.sequence(jsons.map(extractInstance))
    } yield instances
  }

  private def handleWsError(fResponse: Future[WSResponse])(implicit ec: ExecutionContext): Attempt[WSResponse] = {
    fResponse.onFailure {
      case e: Exception => Logger.error("Failed to fetch WsResponse", e)
    }
    Attempt.Async.Right(fResponse)
  }

  private[prism] def amiResponseJson(response: WSResponse): Attempt[JsValue] = Attempt.fromEither {
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
      (response.json \ "data" \ "instances").validate[List[JsValue]]
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
    s"$prismUrl/instances?stack=$stack&stage=$stage&app=$app"
  }
}
