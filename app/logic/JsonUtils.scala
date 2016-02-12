package logic

import models.{AMIableError, AMIableErrors, Attempt}
import play.api.Logger
import play.api.libs.json._

object JsonUtils {
  def jsResultToAttempt(errMessage: String)(jsResult: JsResult[List[JsValue]]): Attempt[List[JsValue]] = {
    jsResult match {
      case JsSuccess(ami, _) => Right(ami)
      case JsError(pathErrors) => Left {
        AMIableErrors(pathErrors.flatMap { case (_, errors) =>
          errors.map { error =>
            Logger.warn(error.message)
            AMIableError(error.message, errMessage, 500)
          }
        })
      }
    }
  }

  def extractToAttempt[T](errMessage: String)(jsResult: JsResult[T]): Attempt[T] = {
    jsResult match {
      case JsSuccess(instance, _) => Right(instance)
      case JsError(pathErrors) => Left {
        AMIableErrors(pathErrors.flatMap { case (path, errors) =>
          errors.map { error =>
            Logger.warn(s"${error.message} at $path")
            AMIableError(error.message, errMessage, 500)
          }
        })
      }
    }
  }
}
