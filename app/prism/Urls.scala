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

  def instancesUrl(stack: Option[String], stage: Option[String], app: Option[String], prismUrl: String) = {
    val getVars = for {
      (name, strOpt) <- List("stack" -> stack, "stage" -> stage, "app" -> app)
      getVar <- strOpt.map(str =>  s"$name=${URLEncoder.encode(str, "UTF-8")}")
    } yield getVar
    s"$prismUrl/instances?${getVars.mkString("&")}"
  }
}
