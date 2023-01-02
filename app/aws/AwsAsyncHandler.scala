package aws

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import play.api.Logging

import scala.concurrent.{Future, Promise}

class AwsAsyncPromiseHandler[R <: AmazonWebServiceRequest, T](
    promise: Promise[T]
) extends AsyncHandler[R, T]
    with Logging {
  def onError(e: Exception) = {
    logger.warn("Failed to execute AWS SDK operation", e)
    promise failure e
  }
  def onSuccess(r: R, t: T) = {
    promise success t
  }
}

object AwsAsyncHandler {
  def awsToScala[R <: AmazonWebServiceRequest, T](
      sdkMethod: Function2[
        R,
        AsyncHandler[R, T],
        java.util.concurrent.Future[T]
      ]
  ): Function1[R, Future[T]] = { req =>
    val p = Promise[T]
    sdkMethod(req, new AwsAsyncPromiseHandler(p))
    p.future
  }
}
