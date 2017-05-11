package services.notification

import config.AMIableConfig
import models.{Attempt, Instance, Owner, SSA}
import play.api.Logger
import prism.{Prism, PrismLogic}

import scala.concurrent.{ExecutionContext, Future}

object ScheduledNotificationRunner {

  def run(mailClient: AWSMailClient)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[String]] = {
    for {
      instancesWithAmis <- Prism.instancesWithAmis(SSA(stage = Some("PROD")))
      oldInstances = PrismLogic.oldInstances(instancesWithAmis)
      _ = Logger.debug(s"Found ${oldInstances.size} PROD instances running on an out-of-date AMI")
      owners <- Prism.getOwners
      mailIds <- Attempt.traverse(owners)(owner => mailClient.send(owner, instancesForOwner(owner, oldInstances)))
    } yield mailIds.collect { case Some(m) => m }
  }

  def instancesForOwner(owner: Owner, oldInstances: List[Instance]): List[Instance] = {
    oldInstances.filter(i => {
      owner.stacks.exists(ssa => ssa == SSA(i.stack, i.stage, i.app.headOption) || ssa == SSA(i.stack, stage = None, i.app.headOption) ||
        ssa == SSA(i.stack, i.stage, app = None) || ssa == SSA(i.stack))

    })
  }
}
