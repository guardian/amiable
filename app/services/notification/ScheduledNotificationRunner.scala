package services.notification

import config.AMIableConfig
import models.{Instance, Owner, SSA}
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
            owners.map { owner =>
              mailClient.send(owner.id, instancesForOwner(owner, oldInstances))
            }
        })
      }
    )
  }

  def instancesForOwner(owner: Owner, oldInstances: List[Instance]): List[Instance] = {
    oldInstances.filter(i => owner.stacks.contains(SSA(i.stack, i.stage, i.app.headOption)))
  }
}
