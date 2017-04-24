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
            owners.map { owner =>
              val email = infoForOwner(owner, oldInstances)
              mailClient.send(email)
            }
        })
      }
    )
  }

  def infoForOwner(owner: Owner, oldInstances: List[Instance]): Email = {
    val ssas = owner.stacks
    val oldInstancesForOwner = oldInstances.filter(i => ssas.contains(SSA(i.stack, i.stage, i.app.headOption)))
    val ownerAddress = s"${owner.id}@guardian.co.uk"
    Email(ownerAddress, "Report: Outdated instances running on PROD", createReport(oldInstancesForOwner))
  }

  def createReport(oldInstances: List[Instance]) :String = {
    "The following instances are running using old AMIs (older than 30 days):\n" + oldInstances.mkString(",\n")
  }
}
