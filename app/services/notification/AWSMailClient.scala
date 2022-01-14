package services.notification

import aws.AwsAsyncHandler.awsToScala
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync
import com.amazonaws.services.simpleemail.model._

import javax.inject.Inject
import models._
import play.api.Logging

import scala.concurrent.ExecutionContext

class AWSMailClient @Inject()(amazonMailClient: AmazonSimpleEmailServiceAsync) (implicit exec: ExecutionContext) extends Logging {

  def send(toAddress: String, request: SendEmailRequest): Attempt[String] = {

    val messageId = awsToScala(amazonMailClient.sendEmailAsync)(request).map(_.getMessageId)

    request.setDestination(new Destination().withToAddresses(toAddress))

    Attempt.future(messageId) { case e =>
      logger.warn("Failed to send email", e)
      Left(AMIableErrors(AMIableError("Error sending email", "Error sending email, please try again.", 500)))
    }

  }
}
