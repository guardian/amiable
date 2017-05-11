package services.notification

import javax.inject.{Inject, Singleton}

import config.{AMIableConfig, AmiableConfigProvider}
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder.newJob
import org.quartz.{JobKey, TriggerKey}
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Logger, Mode}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Notifications @Inject()(
                               amiableConfigProvider: AmiableConfigProvider,
                               environment: Environment,
                               lifecycle: ApplicationLifecycle,
                               mailClient: AWSMailClient)
                             (implicit exec: ExecutionContext,
                              configuration: Configuration) {
  implicit val conf: AMIableConfig = amiableConfigProvider.conf
  /**
    * a Quartz cron expression,
    * e.g. "0 0 3 * * ? *" to run at 3am every day
    * See http://www.quartz-scheduler.org/documentation/quartz-2.2.x/tutorials/crontrigger.html
    */
  private val ownerSchdlCron = configuration.getString("amiable.owner.notification.cron").get
  private val scheduler = StdSchedulerFactory.getDefaultScheduler

  // send notifications only in PROD
  if (environment.mode == Mode.Prod) {
    Logger.info("Starting the scheduler")
    scheduler.start()
    setupSchedule(mailClient)
    lifecycle.addStopHook { () =>
      Logger.info("Shutting down scheduler")
      Future.successful(scheduler.shutdown())
    }
  }

  def setupSchedule(mailClient: AWSMailClient)(implicit config: AMIableConfig, ec: ExecutionContext, configuration: Configuration): Unit = {
    scheduler.getContext.put("MailClient", mailClient)
    scheduler.getContext.put("AMIableConfig", config)
    scheduler.getContext.put("ExecutionContext", ec)
    scheduler.getContext.put("Configuration", configuration)
    val jobDetail = newJob(classOf[NotificationJob])
      .withIdentity(jobKey("notificationJob"))
      .build()
    val trigger = newTrigger()
      .withIdentity(triggerKey("notificationTrigger"))
      .withSchedule(cronSchedule(ownerSchdlCron))
      .build()
    scheduler.scheduleJob(jobDetail, trigger)
    Logger.info(s"Scheduled owner notification with schedule [$ownerSchdlCron]")
  }

  private def triggerKey(id: String): TriggerKey = new TriggerKey(id)

  private def jobKey(id: String): JobKey = new JobKey(id)
}
