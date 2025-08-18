package services.notification

import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  DefaultCredentialsProvider,
  ProfileCredentialsProvider,
  InstanceProfileCredentialsProvider
}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesAsyncClient

object AmazonSimpleEmailServiceAsyncFactory {

  private lazy val provider: AwsCredentialsProvider = {
    import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
    
    val profileProvider = ProfileCredentialsProvider.builder()
      .profileName("deployTools")
      .build()
    
    val instanceProvider = InstanceProfileCredentialsProvider.builder()
      .build()
    
    AwsCredentialsProviderChain.builder()
      .addCredentialsProvider(profileProvider)
      .addCredentialsProvider(instanceProvider)
      .build()
  }

  private val region: Region = Region.EU_WEST_1

  def amazonSimpleEmailServiceAsync: SesAsyncClient =
    SesAsyncClient.builder()
      .region(region)
      .credentialsProvider(provider)
      .build()
}
