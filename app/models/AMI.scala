package models

import org.joda.time.DateTime
import play.api.libs.json._

case class AMI(
  arn: String,
  name: String,
  imageId: String,
  region: String,
  description: String,
  tags: Map[String,String],
  creationDate: Option[String],
  state: String,
  architecture: String,
  ownerId: String,
  virtualizationType: String,
  hypervisor: String,
  sriovNetSupport: String
)

object AMI {
  implicit val jsonFormat = Json.format[AMI]
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
}
