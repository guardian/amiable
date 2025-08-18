package services.notification

import aws.AwsAsyncHandler.awsToScala
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model._

import javax.inject.Inject
import models._
import play.api.Logging

import scala.concurrent.ExecutionContext

class AWSMailClient @Inject() (amazonMailClient: SesAsyncClient)(
    implicit exec: ExecutionContext
) extends Logging {

  def send(toAddress: String, request: SendEmailRequest): Attempt[String] = {

    val updatedRequest = request.toBuilder()
      .destination(Destination.builder()
        .toAddresses(toAddress)
        .build())
      .build()

    val messageId = awsToScala(amazonMailClient.sendEmail(updatedRequest))
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
