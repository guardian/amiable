package services.notification

import javax.inject.Inject

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync
import com.amazonaws.services.simpleemail.model._
import models.{Instance, Owner}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.{Configuration, Logger}

import scala.concurrent.{Future, Promise}

class AWSMailClient @Inject()(amazonMailClient: AmazonSimpleEmailServiceAsync, configuration: Configuration) {

  def send(owner: Owner, instances: Seq[Instance]): Future[String] = {
    val request: SendEmailRequest = createEmailRequest(owner, instances)

    val promise = Promise[SendEmailResult]()
    val responseHandler = new AsyncHandler[SendEmailRequest, SendEmailResult] {
      override def onError(e: Exception): Unit = {
        Logger.warn(s"Could not send mail: ${e.getMessage}\n" +
          s"The failing request was $request")
        promise.failure(e)
      }

      override def onSuccess(request: SendEmailRequest, result: SendEmailResult): Unit = {
        Logger.info("The email has been successfully sent")
        promise.success(result)
      }
    }
    amazonMailClient.sendEmailAsync(request, responseHandler)
    promise.future.map(_.getMessageId)
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
