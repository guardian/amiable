package services.notification

import javax.inject.Inject

import aws.AwsAsyncHandler.awsToScala
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync
import com.amazonaws.services.simpleemail.model._
import models.{Attempt, Instance, Owner}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.{Configuration, Logger}

class AWSMailClient @Inject()(amazonMailClient: AmazonSimpleEmailServiceAsync, configuration: Configuration) {

  def send(owner: Owner, instances: Seq[Instance]): Attempt[Option[String]] = {
    val request: SendEmailRequest = createEmailRequest(owner, instances)
    val messageId = awsToScala(amazonMailClient.sendEmailAsync)(request).map(_.getMessageId)
    Attempt.fromFuture[Option[String]]( messageId.map(m => Right(Some(m)))  ) {
      case e =>
        Logger.warn("Failed to send email", e)
        Right(None)
    }
  }

  private def createEmailRequest(owner: Owner, instances: Seq[Instance]) = {
    val fromAddress = configuration.getString("amiable.mailClient.fromAddress").get
    val destination = new Destination().withToAddresses(s"${owner.id}@guardian.co.uk")
    val emailSubject = new Content().withData("Instances running using old AMIs (older than 30 days)")
    val htmlBody = new Content().withData(views.html.email(instances, owner).toString())
    val body = new Body().withHtml(htmlBody)
    val emailMessage = new Message().withSubject(emailSubject).withBody(body)
    val request = new SendEmailRequest().withSource(fromAddress).withDestination(destination).withMessage(emailMessage)
    request
  }
}
