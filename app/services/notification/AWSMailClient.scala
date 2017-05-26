package services.notification

import javax.inject.Inject

import aws.AwsAsyncHandler.awsToScala
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync
import com.amazonaws.services.simpleemail.model._
import models._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

class AWSMailClient @Inject()(amazonMailClient: AmazonSimpleEmailServiceAsync) {

  def send(toAddress: String, request: SendEmailRequest): Attempt[String] = {
    val messageId = awsToScala(amazonMailClient.sendEmailAsync)(request).map(_.getMessageId)
    request.setDestination(new Destination().withToAddresses(toAddress))
    Attempt.future(messageId) { case e =>
      Logger.warn("Failed to send email", e)
      Left(AMIableErrors(AMIableError("Error sending email", "Error sending email, please try again.", 500)))
    }
  }
}
