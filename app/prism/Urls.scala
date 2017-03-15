package prism

import java.net.URLEncoder

import models.SSA


object Urls {
  def amiUrl(arn: String, prismUrl: String): String = {
    val encodedArn = URLEncoder.encode(arn, "UTF-8")
    s"$prismUrl/images/$encodedArn"
  }

  def amisUrl(prismUrl: String): String = {
    s"$prismUrl/images"
  }

  private[prism] def emptyToNone(strOpt: Option[String]) = strOpt.filter(_.nonEmpty)

  def instancesUrl(ssa: SSA, prismUrl: String) = {
    val getVars = for {
      (name, strOpt) <- List("stack" -> ssa.stack, "stage" -> ssa.stage, "app" -> ssa.app)
      getVar <- strOpt.map(str =>  s"$name=${URLEncoder.encode(str, "UTF-8")}")
    } yield getVar
    s"$prismUrl/instances?${getVars.mkString("&")}"
  }

  def imageInstancesUrl(imageId: String, prismUrl: String) = {
    s"$prismUrl/instances?specification.imageId=$imageId"
  }
}
