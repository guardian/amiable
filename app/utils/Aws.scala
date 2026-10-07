package utils

import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  ContainerCredentialsProvider,
  InstanceProfileCredentialsProvider,
  ProfileCredentialsProvider
}
import software.amazon.awssdk.regions.Region

object Aws {
  val region: Region = Region.EU_WEST_1

  val credentialsProvider: AwsCredentialsProvider = {
    import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain

    val profileProvider = ProfileCredentialsProvider
      .builder()
      .profileName("deployTools")
      .build()

    val instanceProvider = InstanceProfileCredentialsProvider
      .builder()
      .build()

    val containerProvider = ContainerCredentialsProvider
      .builder()
      .build()

    AwsCredentialsProviderChain
      .builder()
      .addCredentialsProvider(profileProvider)
      .addCredentialsProvider(instanceProvider)
      .addCredentialsProvider(containerProvider)
      .build()
  }
}
