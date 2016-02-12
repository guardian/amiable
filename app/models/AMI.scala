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
)
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
  stack: String,
  stage: String,
  app: List[String],
  mainclasses: List[String],
  specification: Map[String, String]
)
object Instance {
  import datetime.DateUtils._
  implicit val jsonFormat = Json.format[Instance]
}

case class AMIableError(
  message: String,
  friendlyMessage: String,
  statusCode: Int,
  context: Option[String] = None
)


case class AMIableErrors(errors: List[AMIableError]) {
  def statusCode = errors.map(_.statusCode).max
}
object AMIableErrors {
  def apply(error: AMIableError): AMIableErrors = {
    AMIableErrors(List(error))
  }
  def apply(errors: Seq[AMIableError]): AMIableErrors = {
    AMIableErrors(errors.toList)
  }

  def flip[T](attempts: List[Attempt[T]]): Attempt[List[T]] = {
    attempts.filter(_.isLeft) match {
      case Nil =>
        Right(attempts.map(_.right.get))
      case failures =>
        Left(AMIableErrors(failures.map(_.left.get).flatMap(_.errors)))
    }
  }
}
