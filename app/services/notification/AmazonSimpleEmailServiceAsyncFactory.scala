package services.notification

import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.simpleemail._

object AmazonSimpleEmailServiceAsyncFactory {

  private lazy val provider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("deployTools"),
    new InstanceProfileCredentialsProvider(false))

  private val region: Region = Option(Regions.getCurrentRegion).getOrElse(Region.getRegion(Regions.EU_WEST_1))

  def amazonSimpleEmailServiceAsync: AmazonSimpleEmailServiceAsync =
    AmazonSimpleEmailServiceAsyncClientBuilder.standard()
      .withRegion(region.getName)
      .withCredentials(provider)
      .build()
}
