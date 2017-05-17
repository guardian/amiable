package services.notification

import com.amazonaws.services.simpleemail.model._
import config.AMIableConfig
import models.{Attempt, Instance, Owner, SSA}
import play.api.{Configuration, Logger}
import prism.{Prism, PrismLogic}

import scala.concurrent.ExecutionContext

object ScheduledNotificationRunner {
  // Message id used if an owner doesn't have any old instances, or doesn't own any instances at all
  val MessageNotSent = "0"

  def run(mailClient: AWSMailClient)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[String]] = {
    for {
      instancesWithAmis <- Prism.instancesWithAmis(SSA(stage = Some("PROD")))
      oldInstances = PrismLogic.oldInstances(instancesWithAmis)
      owners <- Prism.getOwners
      mailIds <- Attempt.traverse(owners) { owner =>
        val ownerOldInstances = instancesForOwner(owner, oldInstances)
        if (ownerOldInstances.nonEmpty) {
          val request = createEmailRequest(owner, ownerOldInstances)
          mailClient.send(owner, request)
        } else {
          Logger.info(s"No old instances for owner ${owner.id}")
          Attempt.Right(MessageNotSent)
        }
      }
    } yield mailIds
  }

  def instancesForOwner(owner: Owner, oldInstances: List[Instance]): List[Instance] = {
    oldInstances.filter(i => {
      owner.stacks.exists(ssa => ssa == SSA(i.stack, i.stage, i.app.headOption) || ssa == SSA(i.stack, stage = None, i.app.headOption) ||
        ssa == SSA(i.stack, i.stage, app = None) || ssa == SSA(i.stack))
    })
  }

  private def createEmailRequest(owner: Owner, instances: Seq[Instance])(implicit config: AMIableConfig) = {
    val destination = new Destination().withToAddresses(s"${owner.id}@guardian.co.uk")
    val emailSubject = new Content().withData("Instances running using old AMIs (older than 30 days)")
    val htmlBody = new Content().withData(views.html.email(instances, owner).toString())
    val body = new Body().withHtml(htmlBody)
    val emailMessage = new Message().withSubject(emailSubject).withBody(body)
    val request = new SendEmailRequest().withSource(config.mailAddress).withDestination(destination).withMessage(emailMessage)
    request
  }
}
