package prism

import java.net.URLEncoder

import config.AMIableConfig
import models.{AMI, AMIableError, AMIableErrors, Attempt}
import play.api.Logger
import play.api.libs.json.{Json, JsError, JsSuccess, JsValue}
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

  private[prism] def amiResponseJson(response: WSResponse): Attempt[JsValue] = {
    (response.json \ "data").toEither
      .left.map { valErr =>
        Logger.warn(valErr.message)
        AMIableErrors(AMIableError(valErr.message, "Could not parse AMI response JSON", 500))
      }
  }

  private[prism] def amisResponseJson(response: WSResponse): Attempt[List[JsValue]] = {
    (response.json \ "data" \ "images").validate[List[JsValue]] match {
      case JsSuccess(ami, _) => Right(ami)
      case JsError(pathErrors) => Left {
        AMIableErrors(pathErrors.flatMap { case (_, errors) =>
          errors.map { error =>
            Logger.warn(error.message)
            AMIableError(error.message, "Could not get AMI from response JSON", 500)
          }
        })
      }
    }
  }

  private[prism] def extractAMI(json: JsValue): Attempt[AMI] = {
    json.validate[AMI] match {
      case JsSuccess(ami, _) => Right(ami)
      case JsError(pathErrors) => Left {
        AMIableErrors(pathErrors.flatMap { case (_, errors) =>
          errors.map { error =>
            Logger.warn(s"${error.message}, ${Json.stringify(json)}")
            AMIableError(error.message, "Could not get AMI from response JSON", 500)
          }
        })
      }
    }
  }

  private[prism] def amiUrl(arn: String, prismUrl: String): String = {
    val encodedArn = URLEncoder.encode(arn, "UTF-8")
    s"$prismUrl/images/$encodedArn"
  }

  private[prism] def amisUrl(prismUrl: String): String = {
    s"$prismUrl/images"
  }
}
