package prism

import config.AMIableConfig
import models._
import prism.JsonUtils._
import prism.Urls._
import utils.Percentiles

import scala.concurrent.ExecutionContext

object Prism {
  import PrismLogic._

  def getAMI(arn : String)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[AMI] = {
    val url = amiUrl(arn, config.prismUrl)
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch AMI", url)
      json <- amiResponseJson(response)
      ami <- extractAMI(json)
    } yield ami
  }

  def getAMIs()(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[AMI]] = {
    val url = amisUrl(config.prismUrl)
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch AMIs", url)
      jsons <- amisResponseJson(response)
      amis <- Attempt.sequence(jsons.map(extractAMI))
    } yield amis
  }

  def getInstances(stackStageApp: SSA)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[Instance]] = {
    val url = instancesUrl(stackStageApp, config.prismUrl)
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch instance", url)
      jsons <- instancesResponseJson(response)
      instances <- Attempt.sequence(jsons.map(extractInstance))
    } yield instances
  }

  private def instancesAndAmis(stackStageApp: SSA)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[(List[Instance], List[AMI])] = {
    for {
      prodInstances <- getInstances(stackStageApp)
      amiAttempts = amiArns(prodInstances).map(getAMI)
      amis <- Attempt.successfulAttempts(amiAttempts)
    } yield (prodInstances, amis)
  }

  def instancesWithAmis(stackStageApp: SSA)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[(Instance, Option[AMI])]] = {
    instancesAndAmis(stackStageApp).map { case (instances, amis)  => instanceAmis(instances, amis)}
  }

  def amiWithInstances(stackStageApp: SSA)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[AMI]] = {
    instancesAndAmis(stackStageApp).map { case (instances, amis)  => amiInstances(amis, instances)}
  }

  def instancesAmisAgePercentiles(stackStageApp: SSA)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[Percentiles] = {
    amiWithInstances(stackStageApp).map(PrismLogic.instancesAmisAgePercentiles)
  }
}
