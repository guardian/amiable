package services.notification

import config.AMIableConfig
import models.{Email, Instance, Owner, SSA}
import play.api.Logger
import prism.{Prism, PrismLogic}

import scala.concurrent.{ExecutionContext, Future}

object ScheduledNotificationRunner {

  def run(mailClient: MailClient)(implicit config: AMIableConfig, ec: ExecutionContext): Future[Any] = {
    Prism.instancesWithAmis(SSA(stage = Some("PROD"))).fold(
      { err =>
        Logger.warn(s"Failed to retrieve instance info ${err.logString}")
      },
      { instancesWithAmis =>
        val oldInstances = PrismLogic.oldInstances(instancesWithAmis)
        Logger.debug(s"Found ${oldInstances.size} PROD instances running on an out-of-date AMI")
        Prism.getOwners.fold ({
          err =>
            Logger.warn(s"Failed to get Owners ${err.logString}")
        },{
          owners =>
            val emails = 0//EmailFormatter()
              emails
          //            notifyOwnersAgent.send(emails.toSet)
        })
      }
    )
  }
}
