package services.notification

import javax.inject.Inject

import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.simpleemail._
import com.google.inject.Provider


class AmazonSimpleEmailServiceAsyncFactory extends Provider[AmazonSimpleEmailServiceAsync]{

  private lazy val provider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("deployTools"),
    new DefaultAWSCredentialsProviderChain())

  private val region: Region = Option(Regions.getCurrentRegion).getOrElse(Region.getRegion(Regions.EU_WEST_1))

  override def get(): AmazonSimpleEmailServiceAsync = {
    AmazonSimpleEmailServiceAsyncClientBuilder.standard()
      .withRegion(region.getName)
      .withCredentials(provider)
    .build()
  }
}
