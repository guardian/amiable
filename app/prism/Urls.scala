package prism

import java.net.URLEncoder

import models.SSAA

object Urls {
  def amiUrl(arn: String, prismUrl: String): String = {
    val encodedArn = URLEncoder.encode(arn, "UTF-8")
    s"$prismUrl/images/$encodedArn"
  }

  def amisUrl(prismUrl: String): String = {
    s"$prismUrl/images"
  }

  def ownersUrl(prismUrl: String): String = {
    s"$prismUrl/owners"
  }

  private[prism] def emptyToNone(strOpt: Option[String]) =
    strOpt.filter(_.nonEmpty)

  def instancesUrl(ssaa: SSAA, prismUrl: String) = {
    val getVars = for {
      (name, strOpt) <- List(
        "stack" -> ssaa.stack,
        "stage" -> ssaa.stage,
        "app" -> ssaa.app,
        "meta.origin.accountName" -> ssaa.accountName
      )
      getVar <- strOpt.map(str => s"$name=${URLEncoder.encode(str, "UTF-8")}")
    } yield getVar
    s"$prismUrl/instances?${getVars.mkString("&")}"
  }

  def imageInstancesUrl(imageId: String, prismUrl: String) = {
    s"$prismUrl/instances?specification.imageId=$imageId"
  }

  def imageLaunchConfigUrl(imageId: String, prismUrl: String) = {
    s"$prismUrl/launch-configurations?imageId=$imageId"
  }
}
