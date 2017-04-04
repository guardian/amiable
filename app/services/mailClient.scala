package services

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClient
import com.amazonaws.services.simpleemail.model._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.{Future, Promise}

trait MailClient {
  def send(email: Email): Future[String]
}

class AWSMailClient(amazonMailClient: AmazonSimpleEmailServiceAsyncClient, fromAddress: String) extends MailClient {

  def send(email: Email): Future[String] = {
    val destination = new Destination().withToAddresses(email.address)
    val emailSubject = new Content().withData(email.subject)
    val textBody = new Content().withData(email.message)
    val body = new Body().withText(textBody)
    val emailMessage = new Message().withSubject(emailSubject).withBody(body)
    val request = new SendEmailRequest().withSource(fromAddress).withDestination(destination).withMessage(emailMessage)

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
}