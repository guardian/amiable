package prism

import java.net.URLEncoder

import config.AMIableConfig
import models._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws._

import scala.concurrent.ExecutionContext

object PrismClient {
  def getAMI(arn : String)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[AMI] = {
    val url = amiUrl(arn, config.prismUrl)
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch AMI info", url)
      json <- amiResponseJson(response)
      ami <- extractAMI(json)
    } yield ami
  }

  def getAMIs()(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[AMI]] = {
    val url = amisUrl(config.prismUrl)
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch data for AMIs", url)
      jsons <- amisResponseJson(response)
      amis <- Attempt.sequence(jsons.map(extractAMI))
    } yield amis
  }

  def getInstances(stack: Option[String], stage: Option[String], app: Option[String])(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[Instance]] = {
    val url = instancesUrl(stack, stage, app, config.prismUrl)
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch instance data", url)
      jsons <- instancesResponseJson(response)
      instances <- Attempt.sequence(jsons.map(extractInstance))
    } yield instances
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

  private[prism] def instancesUrl(stack: Option[String], stage: Option[String], app: Option[String], prismUrl: String) = {
    val getVars = for {
      (name, strOpt) <- List("stack" -> stack, "stage" -> stage, "app" -> app)
      getVar <- strOpt.map(str =>  s"$name=${URLEncoder.encode(str, "UTF-8")}")
    } yield getVar
    s"$prismUrl/instances?${getVars.mkString("&")}"
  }
}
