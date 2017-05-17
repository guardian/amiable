package services.notification

import config.AMIableConfig
import org.quartz.{Job, JobExecutionContext}
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

/** Quartz job wrapper for [[ScheduledNotificationRunner]] */
class NotificationJob extends Job {

  override def execute(context: JobExecutionContext): Unit = {
    val schedulerContext = context.getScheduler.getContext
    val mailClient = schedulerContext.get("MailClient").asInstanceOf[AWSMailClient]
    implicit val amiableConfig = schedulerContext.get("AMIableConfig").asInstanceOf[AMIableConfig]
    implicit val ec = schedulerContext.get("ExecutionContext").asInstanceOf[ExecutionContext]
    val result = ScheduledNotificationRunner.run(mailClient)
    Await.result(result.asFuture, 1.minute).fold(
      err => Logger.error(s"Failed to send scheduled notifications: ${err.logString}"), { msgs =>
        val (notSent, sent) = msgs.partition(_ == ScheduledNotificationRunner.MessageNotSent)
        Logger.info(s"Email notifications: ${sent.size} sent, ${notSent.size} not sent")
      }
    )
  }
}
