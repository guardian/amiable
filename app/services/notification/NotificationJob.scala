package services.notification

import config.AMIableConfig
import org.quartz.{Job, JobExecutionContext, SchedulerContext}
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

/** Quartz job wrapper for [[ScheduledNotificationRunner]] */
class NotificationJob extends Job {

  override def execute(context: JobExecutionContext): Unit = {
    val schedulerContext = context.getScheduler.getContext
    val scheduledNotificationRunner = schedulerContext.get("ScheduledNotificationRunner").asInstanceOf[ScheduledNotificationRunner]
    implicit val ec = schedulerContext.get("ExecutionContext").asInstanceOf[ExecutionContext]
    val result = scheduledNotificationRunner.run()
    Await.result(result.asFuture, 10.minutes).fold(
      err => Logger.error(s"Failed to send scheduled notifications: ${err.logString}"), { msgs =>
        Logger.info(s"Email notifications: ${msgs.size} sent")
      }
    )
  }
}
