package services.notification

import javax.inject.Inject

import aws.AwsAsyncHandler.awsToScala
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync
import com.amazonaws.services.simpleemail.model._
import models.{Attempt, Instance, Owner}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.{Configuration, Logger}

class AWSMailClient @Inject()(amazonMailClient: AmazonSimpleEmailServiceAsync) {

  def send(owner: Owner, request: SendEmailRequest): Attempt[Option[String]] = {
    val messageId = awsToScala(amazonMailClient.sendEmailAsync)(request).map(_.getMessageId)
    Attempt.fromFuture[Option[String]]( messageId.map(m => Right(Some(m)))  ) {
      case e =>
        Logger.warn("Failed to send email", e)
        Right(None)
    }
  }
}
