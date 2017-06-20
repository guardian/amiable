package services.notification

import org.joda.time.DateTime
import org.quartz.{Job, JobExecutionContext}
import play.api.Logger

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

/** Quartz job wrapper for [[ScheduledNotificationRunner]] */
class NotificationJob extends Job {

  override def execute(context: JobExecutionContext): Unit = {
    val schedulerContext = context.getScheduler.getContext
    val scheduledNotificationRunner = schedulerContext.get("ScheduledNotificationRunner").asInstanceOf[ScheduledNotificationRunner]
    implicit val ec = schedulerContext.get("ExecutionContext").asInstanceOf[ExecutionContext]
    val result = scheduledNotificationRunner.run(new DateTime())
    Await.result(result.asFuture, 10.minutes).fold(
      err => Logger.error(s"Failed to send scheduled notifications: ${err.logString}"), { msgs =>
        Logger.info(s"Email notifications: ${msgs.size} sent")
      }
    )
  }
}
