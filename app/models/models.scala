package models

import org.joda.time.DateTime
import play.api.libs.json._
import utils.{DateUtils, Percentiles}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class AMI(
  arn: String,
  name: Option[String],
  imageId: String,
  region: String,
  description: Option[String],
  tags: Map[String,String],
  creationDate: Option[DateTime],
  state: String,
  architecture: String,
  ownerId: String,
  virtualizationType: String,
  hypervisor: String,
  rootDeviceType: String,
  sriovNetSupport: Option[String],
  upgrade: Option[AMI] = None
) {
  override def toString: String = s"AMI<$arn>"
}

object AMI {
  import utils.DateUtils._
  implicit val jsonFormat = Json.format[AMI]

  def extract(imageId: String, amis: List[AMI]): Attempt[AMI] = {
    val imageOpt = amis.find(_.imageId == imageId)
    Attempt.fromOption(imageOpt, AMIableErrors(AMIableError(s"Could not find image with id: $imageId", s"Could not find image with id: $imageId", 404)))
  }
}

case class Instance(
  arn: String,
  name: String,
  vendorState: String,
  group: String,
  dnsName: String,
  ip: String,
  createdAt: DateTime,
  instanceName: String,
  region: String,
  vendor: String,
  securityGroups: List[String],
  tags: Map[String, String],
  stack: Option[String],
  stage: Option[String],
  app: List[String],
  mainclasses: List[String],
  specification: Map[String, String],
  meta: Meta
) {
  val amiArn = specification.get("imageArn")

  override def toString: String = s"Instance<$arn>"
}

case class Origin(
  vendor: String,
  accountName: String,
  region: String,
  accountNumber: String)

object Origin {
  implicit val jsonFormat = Json.format[Origin]
}

case class Meta(
  href: String,
  origin: Origin
)

object Meta {
  implicit val jsonFormat = Json.format[Meta]
}

case class SSAA (
  stack: Option[String] = None,
  stage: Option[String] = None,
  app: Option[String] = None,
  accountName: Option[String] = None
) {
  def isEmpty = stack.isEmpty && stage.isEmpty && app.isEmpty
  override def toString: String = {
    // accountName is non optional in some cases so is often set to "". In these cases treat it as 'none'
    val accountString = if (accountName.isEmpty || accountName.contains(""))"unknown-account" else accountName.get
    s"SSAA<${stack.getOrElse("none")}, ${stage.getOrElse("none")}, ${app.getOrElse("none")}, ${accountString}>"
  }
}
object SSAA {
  implicit val jsonFormat = Json.format[SSAA]

  /**
    * Filters empty strings to None, such as those provided by request parameters.
    */
  def fromParams(stack: Option[String] = None, stage: Option[String] = None, app: Option[String] = None, accountName: Option[String] = None): SSAA =
    SSAA(stack.filter(_.nonEmpty), stage.filter(_.nonEmpty), app.filter(_.nonEmpty), accountName.filter(_.nonEmpty))

  def empty = SSAA(None, None, None, None)

  def riffRaffLink(ssaa: SSAA, region: String): Option[String] = for {
    stack <- ssaa.stack
    stage <- ssaa.stage
    app <- ssaa.app
  } yield {
    s"https://riffraff.gutools.co.uk/deployment/target/deploy?region=$region&stack=$stack&stage=$stage&app=$app"
  }
}

case class AMIableError(
  message: String,
  friendlyMessage: String,
  statusCode: Int,
  context: Option[String] = None
)

sealed trait Age
object Fresh extends Age
object Turning extends Age
object Old extends Age

case class Metrics(oldInstancesCount: Int, totalInstancesCount: Int, agePercentiles: Percentiles)
case class ChartTimeSerie(label: String, data: List[(DateTime, Double)], color: String = "")
case class Chart(title: String, data: List[ChartTimeSerie]) {
  val id = title.hashCode
}

case class LaunchConfiguration(
  arn: String,
  name: String,
  imageId: String,
  region: String,
  createdTime: DateTime,
  instanceType: String,
  keyName: String,
  securityGroups: List[String],
  userData: Option[String],
  meta: Meta)

object LaunchConfiguration {
  import utils.DateUtils._
  implicit val jsonFormat = Json.format[LaunchConfiguration]
}

case class Owner(id: String, stacks: List[SSAA]) {
  def hasSSA(ssaa: SSAA): Boolean = stacks.contains(ssaa)
}

object Owner {
  implicit val jsonFormat = Json.format[Owner]
}

case class Owners(owners: List[Owner], defaultOwner: Owner)

object Owners {
  implicit val jsonFormat = Json.format[Owners]
}

case class AWSAccount(accountNumber: Option[String], accountName: String)
object AWSAccount {
  implicit val jsonFormat = Json.format[AWSAccount]
}
