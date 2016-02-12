package logic

import models.{AMIableError, AMIableErrors, Attempt}
import play.api.Logger
import play.api.libs.json._

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
}
