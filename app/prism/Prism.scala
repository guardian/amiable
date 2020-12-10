package prism

import config.AMIableConfig
import models._
import prism.JsonUtils._
import prism.Urls._
import utils.Percentiles

import scala.concurrent.ExecutionContext

object Prism {
  import PrismLogic._

  def getOwners(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[Owners] = {
    val url = ownersUrl(config.prismUrl)
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch Owners", url)
      ownersJson <- ownersResponseJson(response)
      owners <- extractOwners(ownersJson)
    } yield owners
  }

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

  def getInstances(stackStageApp: SSAA)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[Instance]] = {
    val url = instancesUrl(stackStageApp, config.prismUrl)
    getInstancesFromUrl(url)
  }

  def imageUsage(image: AMI)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[Instance]] = {
    val url = imageInstancesUrl(image.imageId, config.prismUrl)
    getInstancesFromUrl(url)
  }

  private def getInstancesFromUrl(url: String)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[Instance]] = {
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch instance", url)
      jsons <- instancesResponseJson(response)
      instances <- Attempt.sequence(jsons.map(extractInstance))
    } yield instances
  }

  def instancesWithAmis(stackStageApp: SSAA)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[(Instance, Option[AMI])]] = {
    for {
      prodInstances <- getInstances(stackStageApp)
      amiAttempts = amiArns(prodInstances).map(getAMI)
      amis <- Attempt.successfulAttempts(amiAttempts)
    } yield instanceAmis(prodInstances, amis)
  }

  def launchConfigUsage(image: AMI)(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[LaunchConfiguration]] = {
    val url = imageLaunchConfigUrl(image.imageId, config.prismUrl)
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch launch configurations", url)
      jsons <- launchConfigurationResponseJson(response)
      launchConfigurations <- Attempt.sequence(jsons.map(extractLaunchConfiguration))
    } yield launchConfigurations
  }

  def getAccounts(implicit config: AMIableConfig, ec: ExecutionContext): Attempt[List[AWSAccount]] = {
    val url = s"${config.prismUrl}/sources/accounts"
    for {
      response <- Http.response(config.wsClient.url(url).get(), "Unable to fetch accounts list", url)
      jsons <- accountsResponseJson(response)
      accounts <- Attempt.sequence(jsons.map(extractAccounts))
    } yield accounts
  }
}
