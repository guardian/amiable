package services.notification

import javax.inject.{Inject, Singleton}

import config.{AMIableConfig, AmiableConfigProvider}
import play.api.inject.ApplicationLifecycle
import play.api.{Environment, Logger, Mode}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Notifications @Inject()(amiableConfigProvider: AmiableConfigProvider, environment: Environment, lifecycle: ApplicationLifecycle, mailClient: MailClient)(implicit exec: ExecutionContext) {
   implicit val conf: AMIableConfig = amiableConfigProvider.conf

  // send notifications only in PROD
  if (environment.mode == Mode.Prod) {
    Logger.info("Starting the scheduler")
    NotificationScheduler.start()
    NotificationScheduler.setupSchedule(mailClient)
    lifecycle.addStopHook { () =>
      println("Shutting down scheduler")
      Future.successful(NotificationScheduler.shutdown())
    }
  }
}
