package prism

import models.{AMIableError, AMIableErrors, Attempt}
import play.api.Logging
import play.api.libs.ws.WSResponse

import scala.concurrent.{ExecutionContext, Future}

object Http extends Logging {

  /** Reusable logic for handling WS errors
    */
  def response(
      response: Future[WSResponse],
      friendlyMessage: String,
      detail: String
  )(implicit ec: ExecutionContext): Attempt[WSResponse] = {
    val handleStatusCode: Future[Either[AMIableErrors, WSResponse]] = {
      response.map { response =>
        response.status match {
          case status if status < 400 =>
            Right(response)
          case status =>
            logger.warn(s"Status code $status: $detail")
            Left(
              AMIableErrors(
                AMIableError(s"$status: $detail", friendlyMessage, status)
              )
            )
        }
      }
    }
    Attempt.fromFuture(handleStatusCode) { case e: Exception =>
      logger.error(s"Request failed: $detail", e)
      Left(
        AMIableErrors(
          AMIableError(
            s"Request failed: $detail, ${e.getMessage}",
            friendlyMessage,
            500
          )
        )
      )
    }
  }
}
