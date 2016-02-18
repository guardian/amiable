package prism

import config.AMIableConfig
import models._
import prism.JsonUtils._
import prism.Urls._

import scala.concurrent.ExecutionContext

object Prism {
  def getAMI(arn : String)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[AMI] = {
    val url = amiUrl(arn, config.prismUrl)
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch AMI info", url)
      json <- amiResponseJson(response)
      ami <- extractAMI(json)
    } yield ami
  }

  def getAMIs()(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[AMI]] = {
    val url = amisUrl(config.prismUrl)
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch data for AMIs", url)
      jsons <- amisResponseJson(response)
      amis <- Attempt.sequence(jsons.map(extractAMI))
    } yield amis
  }

  def getInstances(stack: Option[String], stage: Option[String], app: Option[String])(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[Instance]] = {
    val url = instancesUrl(stack, stage, app, config.prismUrl)
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch instance data", url)
      jsons <- instancesResponseJson(response)
      instances <- Attempt.sequence(jsons.map(extractInstance))
    } yield instances
  }
}
