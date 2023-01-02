package services.notification

import config.{AMIableConfig, AmiableConfigProvider}

import javax.inject.Inject
import models.Attempt
import org.joda.time.DateTime
import org.quartz.{JobKey, TriggerKey}
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import play.api.{Environment, Logging}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}

class Notifications @Inject() (
    amiableConfigProvider: AmiableConfigProvider,
    environment: Environment,
    lifecycle: ApplicationLifecycle,
    scheduledNotificationRunner: ScheduledNotificationRunner
)(implicit exec: ExecutionContext)
    extends Logging {
  val conf: AMIableConfig = amiableConfigProvider.conf

  /** a Quartz cron expression, e.g. "0 0 3 * * ? *" to run at 3am every day See
    * http://www.quartz-scheduler.org/documentation/quartz-2.2.x/tutorials/crontrigger.html
    */
  private val scheduler = StdSchedulerFactory.getDefaultScheduler
  conf.overrideToAddress match {
    case Some(address) =>
      logger.info(
        s"To address is overridden for sending notifications. Notifications will be sent to: $address"
      )
    case None =>
      logger.info(
        s"No to address override configured for sending notifications. Using addresses from Prism"
      )
  }

  conf.ownerNotificationCron match {
    case Some(cron) =>
      logger.info(
        s"Starting the scheduler for sending notifications to stack owners"
      )
      scheduler.start()
      setupSchedule(cron)
      lifecycle.addStopHook { () =>
        logger.info("Shutting down scheduler")
        Future.successful(scheduler.shutdown())
      }
    case None =>
      logger.info(
        "No cron expression. Not starting scheduler for sending notifications to stack owners"
      )
  }

  def setupSchedule(
      ownerSchdlCron: String
  )(implicit ec: ExecutionContext): Unit = {
    if (amiableConfigProvider.stage == "PROD") {
      scheduler.getContext.put(
        "ScheduledNotificationRunner",
        scheduledNotificationRunner
      )
      scheduler.getContext.put("ExecutionContext", ec)
      val jobDetail = newJob(classOf[NotificationJob])
        .withIdentity(new JobKey("notificationJob"))
        .build()
      val trigger = newTrigger()
        .withIdentity(new TriggerKey("notificationTrigger"))
        .withSchedule(cronSchedule(ownerSchdlCron))
        .build()
      scheduler.scheduleJob(jobDetail, trigger)
      logger.info(
        s"Scheduled owner notification with schedule [$ownerSchdlCron]"
      )
    } else {
      logger.info(
        s"Scheduled notifications disabled in ${amiableConfigProvider.stage}"
      )
    }
  }

  def sendEmail(): Attempt[List[String]] = {
    scheduledNotificationRunner.run(new DateTime())
  }
}
