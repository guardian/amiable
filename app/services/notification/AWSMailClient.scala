package services.notification

import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model.*

import javax.inject.Inject
import models.*
import play.api.Logging
import utils.Aws

import scala.concurrent.ExecutionContext
import scala.jdk.FutureConverters.*

class AWSMailClient @Inject() (amazonMailClient: SesAsyncClient)(implicit
    exec: ExecutionContext
) extends Logging {

  def send(toAddress: String, request: SendEmailRequest): Attempt[String] = {

    val requestWithDestination = request
      .toBuilder()
      .destination(
        Destination
          .builder()
          .toAddresses(toAddress)
          .build()
      )
      .build()

    val messageId = amazonMailClient
      .sendEmail(requestWithDestination)
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
object AWSMailClient {
  def amazonMailClient: SesAsyncClient =
    SesAsyncClient
      .builder()
      .region(Aws.region)
      .credentialsProvider(Aws.credentialsProvider)
      .build()
}
