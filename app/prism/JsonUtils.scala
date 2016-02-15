package prism

import models._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.WSResponse


object JsonUtils {
  def jsResultToAttempt(errMessage: String)(jsResult: JsResult[List[JsValue]]): Attempt[List[JsValue]] = {
    jsResult match {
      case JsSuccess(ami, _) => Attempt.Right(ami)
      case JsError(pathErrors) => Attempt.Left {
        AMIableErrors(pathErrors.flatMap { case (path, errors) =>
          errors.map { error =>
            Logger.warn(s"${error.message} while extracting list of JsValues at $path")
            AMIableError(error.message, errMessage, 500)
          }
        })
      }
    }
  }

  def extractToAttempt[T](errMessage: String)(jsResult: JsResult[T]): Attempt[T] = {
    jsResult match {
      case JsSuccess(instance, _) => Attempt.Right(instance)
      case JsError(pathErrors) => Attempt.Left {
        AMIableErrors(pathErrors.flatMap { case (path, errors) =>
          errors.map { error =>
            Logger.warn(s"${error.message} extracting value at $path")
            AMIableError(error.message, errMessage, 500)
          }
        })
      }
    }
  }

  def amiResponseJson(response: WSResponse): Attempt[JsValue] = Attempt.fromEither {
    (response.json \ "data").toEither
      .left.map { valErr =>
      Logger.warn(valErr.message)
      AMIableErrors(AMIableError(valErr.message, "Could not parse AMI response JSON", 500))
    }
  }

  def amisResponseJson(response: WSResponse): Attempt[List[JsValue]] = {
    JsonUtils.jsResultToAttempt("Could not get AMI from response JSON"){
      (response.json \ "data" \ "images").validate[List[JsValue]]
    }
  }

  def extractAMI(json: JsValue): Attempt[AMI] = {
    JsonUtils.extractToAttempt[AMI]("Could not get AMI from response JSON") {
      json.validate[AMI]
    }
  }

  def instancesResponseJson(response: WSResponse): Attempt[List[JsValue]] = {
    JsonUtils.jsResultToAttempt("Could not get AMI from response JSON"){
      (response.json \ "data" \ "instances").validate[List[JsValue]]
    }
  }

  def extractInstance(json: JsValue): Attempt[Instance] = {
    JsonUtils.extractToAttempt[Instance]("Could not get Instance from response JSON") {
      json.validate[Instance]
    }
  }
}
