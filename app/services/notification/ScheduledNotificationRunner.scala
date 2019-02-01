package services.notification

import com.amazonaws.services.simpleemail.model._
import config.{AMIableConfig, AmiableConfigProvider}
import javax.inject.Inject
import models._
import org.joda.time.DateTime
import play.api.{Environment, Logger, Mode}
import prism.{Prism, PrismLogic}
import utils.DateUtils

import scala.concurrent.ExecutionContext

class ScheduledNotificationRunner @Inject() (mailClient: AWSMailClient, environment: Environment, amiableConfigProvider: AmiableConfigProvider)(implicit ec: ExecutionContext) {
  implicit val config = amiableConfigProvider.conf

  def run(today: DateTime): Attempt[List[String]] = {
    for {
      instancesWithAmis <- Prism.instancesWithAmis(SSA(stage = Some("PROD")))
      oldInstances = PrismLogic.oldInstances(instancesWithAmis)
      instanceAmiMap = ScheduledNotificationRunner.makeInstanceAmiMap(instancesWithAmis)
      ownersWithDefault <- Prism.getOwners
      ownersAndOldInstances = ScheduledNotificationRunner.findInstanceOwners(oldInstances, ownersWithDefault)
      mailIds <- Attempt.traverse(ownersAndOldInstances.toList) { case (owner, oldInstancesForOwner) =>
        val instances = ScheduledNotificationRunner.pairInstancesWithAmi(oldInstancesForOwner, instanceAmiMap)
        val request = ScheduledNotificationRunner.createEmailRequest(owner, instances, config, today)
        ScheduledNotificationRunner.conditionallySendEmail(environment.mode, config.overrideToAddress, mailClient, owner, request)
      }
    } yield mailIds
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

  def makeInstanceAmiMap(instancesWithAmis: List[(Instance, Option[AMI])]): Map[Instance, AMI] =
    instancesWithAmis.collect{ case (i, Some(ami)) => i -> ami }.toMap

  def pairInstancesWithAmi(instances: List[Instance], instanceAmiMap: Map[Instance, AMI]): List[(Instance, Option[AMI])] = {
    instances
      .map(i => i -> instanceAmiMap.get(i))
      .sortBy{ case (i, maybeAmi) =>
        val maybeCreationTimestamp = maybeAmi.flatMap(_.creationDate).map(_.getMillis)
        (maybeCreationTimestamp, i.stack, i.stage, i.app.headOption)
      }
  }

  def findInstanceOwners(instances: List[Instance], owners: Owners): Map[Owner, List[Instance]] = {
    instances.map { i => (i, ScheduledNotificationRunner.ownerForInstance(i, owners)) }.groupBy(_._2).mapValues(_.map(_._1))
  }

  def createEmailRequest(owner: Owner, instances: Seq[(Instance, Option[AMI])], config: AMIableConfig, today: DateTime): SendEmailRequest = {
    val toAddress = config.overrideToAddress.getOrElse(s"${owner.id}@guardian.co.uk")
    val todaysDate = DateUtils.yearMonthDay.print(today)
    val destination = new Destination().withToAddresses(toAddress)
    val emailSubject = new Content().withData(s"Instances using out of date AMIs (as of $todaysDate, owned by ${owner.id})")
    val htmlBody = new Content().withData(views.html.email(config.amiableUrl, instances, owner).toString())
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
