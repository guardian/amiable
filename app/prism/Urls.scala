package prism

import java.net.URLEncoder


object Urls {
  def amiUrl(arn: String, prismUrl: String): String = {
    val encodedArn = URLEncoder.encode(arn, "UTF-8")
    s"$prismUrl/images/$encodedArn"
  }

  def amisUrl(prismUrl: String): String = {
    s"$prismUrl/images"
  }

  private[prism] def emptyToNone(strOpt: Option[String]) = strOpt.filter(_.nonEmpty)

  def instancesUrl(stack: Option[String], stage: Option[String], app: Option[String], prismUrl: String) = {
    val getVars = for {
      (name, strOpt) <- List("stack" -> emptyToNone(stack), "stage" -> emptyToNone(stage), "app" -> emptyToNone(app))
      getVar <- strOpt.map(str =>  s"$name=${URLEncoder.encode(str, "UTF-8")}")
    } yield getVar
    s"$prismUrl/instances?${getVars.mkString("&")}"
  }
}
