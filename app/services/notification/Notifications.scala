package services.notification

import javax.inject.{Inject, Singleton}

import play.api.{Environment, Logger, Mode}
import play.api.inject.ApplicationLifecycle
import services.Agents

import scala.concurrent.Future

@Singleton
class Notifications @Inject()(environment: Environment, lifecycle: ApplicationLifecycle, mailClient: MailClient) {
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
