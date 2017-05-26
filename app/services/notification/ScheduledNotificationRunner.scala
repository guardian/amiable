package services.notification

import javax.inject.{Inject, Singleton}

import com.amazonaws.services.simpleemail.model._
import config.{AMIableConfig, AmiableConfigProvider}
import models._
import play.api.Mode.Mode
import play.api.{Environment, Logger, Mode}
import prism.{Prism, PrismLogic}

import scala.concurrent.ExecutionContext

@Singleton
class ScheduledNotificationRunner @Inject()(mailClient: AWSMailClient, environment: Environment, amiableConfigProvider: AmiableConfigProvider)(implicit ec: ExecutionContext) {
  implicit val config = amiableConfigProvider.conf

  def run(): Attempt[List[String]] = {
    for {
      instancesWithAmis <- Prism.instancesWithAmis(SSA(stage = Some("PROD")))
      oldInstances = PrismLogic.oldInstances(instancesWithAmis)
      ownersWithDefault <- Prism.getOwners
      ownersAndOldInstances = findInstanceOwners(oldInstances, ownersWithDefault)
      mailIds <- Attempt.traverse(ownersAndOldInstances.toList) { case (owner, oldInstancesForOwner) =>
        val instances = oldInstancesForOwner.sortBy(i => (i.stack, i.stage, i.app.headOption))
        val request = ScheduledNotificationRunner.createEmailRequest(owner, instances, config)
        ScheduledNotificationRunner.conditionallySendEmail(environment.mode, config.overrideToAddress, mailClient, owner, request)
      }
    } yield mailIds
  }

  def findInstanceOwners(instances: List[Instance], owners: Owners): Map[Owner, List[Instance]] = {
    instances.map { i => (i, ScheduledNotificationRunner.ownerForInstance(i, owners)) }.groupBy(_._2).mapValues(_.map(_._1))
  }
}

object ScheduledNotificationRunner {
  def ownerForInstance(i: Instance, owners: Owners): Owner = {
    owners.owners.find(_.hasSSA(SSA(i.stack, i.stage, i.app.headOption)))
      .orElse(owners.owners.find(_.hasSSA(SSA(i.stack, app = i.app.headOption))))
      .orElse(owners.owners.find(_.hasSSA(SSA(i.stack, i.stage))))
      .orElse(owners.owners.find(_.hasSSA(SSA(i.stack))))
      .getOrElse(owners.defaultOwner)
  }

  def createEmailRequest(owner: Owner, instances: Seq[Instance], config: AMIableConfig): SendEmailRequest = {
    val toAddress = config.overrideToAddress.getOrElse(s"${owner.id}@guardian.co.uk")
    val destination = new Destination().withToAddresses(toAddress)
    val emailSubject = new Content().withData("Instances running using old AMIs (older than 30 days)")
    val htmlBody = new Content().withData(views.html.email(instances, owner).toString())
    val body = new Body().withHtml(htmlBody)
    val emailMessage = new Message().withSubject(emailSubject).withBody(body)
    val request = new SendEmailRequest().withSource(config.mailAddress).withDestination(destination).withMessage(emailMessage)
    request
  }

  def conditionallySendEmail(mode: Mode,
                             overrideToAddress: Option[String],
                             mailClient: AWSMailClient,
                             owner: Owner,
                             request: SendEmailRequest): Attempt[String] = {
    (mode, overrideToAddress) match {
      case (Mode.Prod, maybeOverride) =>
        mailClient.send(maybeOverride.getOrElse(s"${owner.id}@guardian.co.uk"), request)
      case (_, Some(overrideToaddress)) =>
        mailClient.send(overrideToaddress, request)
      case (_, None) =>
        Logger.info(s"Not in Prod and no override To Address set. Would have sent email to ${owner.id}, request: $request")
        Attempt.Right("")
    }
  }
}
