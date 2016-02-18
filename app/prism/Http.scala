package prism

import models.{AMIableError, AMIableErrors, Attempt}
import play.api.Logger
import play.api.libs.ws.WSResponse

import scala.concurrent.{ExecutionContext, Future}

object Http {
  /**
    * Reusable logic for handling WS errors
    */
  def response(response: Future[WSResponse], friendlyMessage: String, detail: String)(implicit ec: ExecutionContext): Attempt[WSResponse] = {
    val handleStatusCode: Future[Either[AMIableErrors, WSResponse]] = {
      response.map { response =>
        response.status match {
          case status if status < 400 =>
            Right(response)
          case status =>
            Logger.warn(s"Status code $status: $detail")
            Left(AMIableErrors(
              AMIableError(s"$status: $detail", friendlyMessage, status)
            ))
        }
      }
    }
    Attempt.fromFuture(handleStatusCode) {
      case e: Exception =>
        Logger.error(s"Request failed: $detail", e)
        Left(AMIableErrors(
          AMIableError(s"Request failed: $detail, ${e.getMessage}", friendlyMessage, 500)
        ))
    }
  }
}
