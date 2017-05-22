package services.notification

import com.amazonaws.services.simpleemail.model._
import config.AMIableConfig
import models._
import prism.{Prism, PrismLogic}

import scala.concurrent.ExecutionContext

object ScheduledNotificationRunner {
  def run(mailClient: AWSMailClient)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[String]] = {
    for {
      instancesWithAmis <- Prism.instancesWithAmis(SSA(stage = Some("PROD")))
      oldInstances = PrismLogic.oldInstances(instancesWithAmis)
      ownersWithDefault <- Prism.getOwners
      ownersAndOldInstances = findInstanceOwners(oldInstances, ownersWithDefault)
      mailIds <- Attempt.traverse(ownersAndOldInstances.toList) { case (owner, oldInstancesForOwner) =>
        val request = createEmailRequest(owner, oldInstancesForOwner)
        mailClient.send(owner, request)
      }
    } yield mailIds
  }

  def findInstanceOwners(instances: List[Instance], owners: Owners): Map[Owner, List[Instance]] = {
    instances.map { i => (i, ownerForInstance(i, owners)) }.groupBy(_._2).mapValues(_.map(_._1))
  }

  def ownerForInstance(i: Instance, owners: Owners): Owner = {
    owners.owners.find(_.hasSSA(SSA(i.stack, i.stage, i.app.headOption)))
      .orElse(owners.owners.find(_.hasSSA(SSA(i.stack, app = i.app.headOption))))
      .orElse(owners.owners.find(_.hasSSA(SSA(i.stack, i.stage))))
      .orElse(owners.owners.find(_.hasSSA(SSA(i.stack))))
      .getOrElse(owners.defaultOwner)
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
