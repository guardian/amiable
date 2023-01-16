package prism

import models._
import play.api.Logging
import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext

object JsonUtils extends Logging {

  import Serialisation._

  def jsResultToAttempt(
      errMessage: String
  )(jsResult: JsResult[List[JsValue]]): Attempt[List[JsValue]] = {
    jsResult match {
      case JsSuccess(ami, _) => Attempt.Right(ami)
      case JsError(pathErrors) =>
        Attempt.Left {
          AMIableErrors(pathErrors.flatMap { case (path, errors) =>
            errors.map { error =>
              logger.warn(
                s"${error.message} while extracting list of JsValues at $path"
              )
              AMIableError(error.message, errMessage, 500)
            }
          }.toList)
        }
    }
  }

  def extractToAttempt[T](
      errMessage: String
  )(jsResult: JsResult[T]): Attempt[T] = {
    jsResult match {
      case JsSuccess(instance, _) => Attempt.Right(instance)
      case JsError(pathErrors) =>
        Attempt.Left {
          AMIableErrors(pathErrors.flatMap { case (path, errors) =>
            errors.map { error =>
              logger.warn(s"${error.message} extracting value at $path")
              AMIableError(error.message, errMessage, 500)
            }
          }.toList)
        }
    }
  }

  def amiResponseJson(response: WSResponse): Attempt[JsValue] =
    Attempt.fromEither {
      (response.json \ "data").toEither.left.map { valErr =>
        logger.warn(valErr.message)
        AMIableErrors(
          AMIableError(valErr.message, "Could not parse AMI response JSON", 500)
        )
      }
    }

  def amisResponseJson(response: WSResponse): Attempt[List[JsValue]] = {
    JsonUtils.jsResultToAttempt("Could not get AMI from response JSON") {
      (response.json \ "data" \ "images").validate[List[JsValue]]
    }
  }

  def extractAMI(json: JsValue): Attempt[AMI] = {
    JsonUtils.extractToAttempt[AMI]("Could not get AMI from response JSON") {
      json.validate[AMI]
    }
  }

  def instancesResponseJson(response: WSResponse): Attempt[List[JsValue]] = {
    JsonUtils.jsResultToAttempt("Could not get AMI from response JSON") {
      (response.json \ "data" \ "instances").validate[List[JsValue]]
    }
  }

  def launchConfigurationResponseJson(
      response: WSResponse
  ): Attempt[List[JsValue]] = {
    JsonUtils.jsResultToAttempt(
      "Could not get Launch Configuration from response JSON"
    ) {
      (response.json \ "data" \ "launch-configurations").validate[List[JsValue]]
    }
  }

  def accountsResponseJson(response: WSResponse): Attempt[List[JsValue]] = {
    JsonUtils.jsResultToAttempt(
      "Could not get aws accounts from response JSON"
    ) {
      (response.json \ "data").validate[List[JsValue]]
    }
  }

  def ownersResponseJson(
      response: WSResponse
  )(implicit ec: ExecutionContext): Attempt[JsValue] = {
    JsonUtils.extractToAttempt("Could not get Owners from response JSON") {
      (response.json \ "data").validate[JsValue]
    }
  }

  def extractInstance(json: JsValue): Attempt[Instance] = {
    JsonUtils.extractToAttempt[Instance](
      "Could not get Instance from response JSON"
    ) {
      json.validate[Instance]
    }
  }

  def extractLaunchConfiguration(
      json: JsValue
  ): Attempt[LaunchConfiguration] = {
    JsonUtils.extractToAttempt[LaunchConfiguration](
      "Could not get Launch Configuration from response JSON"
    ) {
      json.validate[LaunchConfiguration]
    }
  }

  def extractAccounts(json: JsValue): Attempt[AWSAccount] = {
    JsonUtils.extractToAttempt[AWSAccount](
      "Could not get Accounts from response JSON"
    ) {
      json.validate[AWSAccount]
    }
  }

  def extractOwners(json: JsValue): Attempt[Owners] = {
    JsonUtils.extractToAttempt[Owners](
      "Could not get owners from response JSON"
    ) {
      json.validate[Owners]
    }
  }
}
