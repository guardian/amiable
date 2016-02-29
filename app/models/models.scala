package models

import org.joda.time.format.DateTimeFormatter

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

import scala.concurrent.Future

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
  sriovNetSupport: Option[String]
) {
  override def toString: String = s"AMI<$arn>"
}
object AMI {
  import datetime.DateUtils._
  implicit val jsonFormat = Json.format[AMI]
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

case class Meta(
  href: String,
  origin: Origin
)

case class Origin(
  vendor: String,
  accountName: String,
  region: String,
  accountNumber: String
)

case class SSA (
  stack: Option[String] = None,
  stage: Option[String] = None,
  app: Option[String] = None
) {
  def isEmpty = stack.isEmpty && stage.isEmpty && app.isEmpty
  override def toString: String = s"SSA<${stack.getOrElse("none")}, ${stage.getOrElse("none")}, ${app.getOrElse("none")}>"
}
object SSA {
  /**
    * Filters empty strings to None, such as those provided by request parameters.
    */
  def fromParams(stack: Option[String] = None, stage: Option[String] = None, app: Option[String] = None): SSA =
    SSA(stack.filter(_.nonEmpty), stage.filter(_.nonEmpty), app.filter(_.nonEmpty))
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
