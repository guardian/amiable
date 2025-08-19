package services.notification

import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model._

import javax.inject.Inject
import models._
import play.api.Logging

import scala.concurrent.ExecutionContext
import scala.jdk.FutureConverters._

class AWSMailClient @Inject() (amazonMailClient: SesAsyncClient)(implicit
    exec: ExecutionContext
) extends Logging {

  def send(toAddress: String, request: SendEmailRequest): Attempt[String] = {

    val updatedRequest = request
      .toBuilder()
      .destination(
        Destination
          .builder()
          .toAddresses(toAddress)
          .build()
      )
      .build()

    val messageId = amazonMailClient
      .sendEmail(updatedRequest)
      .asScala
      .map(_.messageId())

    Attempt.future(messageId) { case e =>
      logger.warn("Failed to send email", e)
      Left(
        AMIableErrors(
          AMIableError(
            "Error sending email",
            "Error sending email, please try again.",
            500
          )
        )
      )
    }

  }
}
