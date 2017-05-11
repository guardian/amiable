package services.notification

import com.amazonaws.services.simpleemail.model._
import config.AMIableConfig
import models.{Attempt, Instance, Owner, SSA}
import play.api.{Configuration, Logger}
import prism.{Prism, PrismLogic}

import scala.concurrent.{ExecutionContext, Future}

object ScheduledNotificationRunner {

  def run(mailClient: AWSMailClient)(implicit config: AMIableConfig, ec: ExecutionContext, configuration: Configuration): Attempt[List[String]] = {
    for {
      instancesWithAmis <- Prism.instancesWithAmis(SSA(stage = Some("PROD")))
      oldInstances = PrismLogic.oldInstances(instancesWithAmis)
      _ = Logger.debug(s"Found ${oldInstances.size} PROD instances running on an out-of-date AMI")
      owners <- Prism.getOwners
      mailIds <- Attempt.traverse(owners)(owner => {
        val request = createEmailRequest(owner, instancesForOwner(owner, oldInstances), configuration)
        mailClient.send(owner, request)
      })
    } yield mailIds.collect { case Some(m) => m }
  }

  def instancesForOwner(owner: Owner, oldInstances: List[Instance]): List[Instance] = {
    oldInstances.filter(i => {
      owner.stacks.exists(ssa => ssa == SSA(i.stack, i.stage, i.app.headOption) || ssa == SSA(i.stack, stage = None, i.app.headOption) ||
        ssa == SSA(i.stack, i.stage, app = None) || ssa == SSA(i.stack))

    })
  }

  private def createEmailRequest(owner: Owner, instances: Seq[Instance], configuration: Configuration) = {
    val fromAddress = configuration.getString("amiable.mailClient.fromAddress").get
    val destination = new Destination().withToAddresses(s"${owner.id}@guardian.co.uk")
    val emailSubject = new Content().withData("Instances running using old AMIs (older than 30 days)")
    val htmlBody = new Content().withData(views.html.email(instances, owner).toString())
    val body = new Body().withHtml(htmlBody)
    val emailMessage = new Message().withSubject(emailSubject).withBody(body)
    val request = new SendEmailRequest().withSource(fromAddress).withDestination(destination).withMessage(emailMessage)
    request
  }
}
