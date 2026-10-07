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
    import software.amazon.awssdk.auth.credentials.{
      AwsCredentialsProviderChain,
      DefaultCredentialsProvider
    }

    val profileProvider = ProfileCredentialsProvider
      .builder()
      .profileName("deployTools")
      .build()

    val defaultProvider = DefaultCredentialsProvider
      .builder()
      .build()

    AwsCredentialsProviderChain
      .builder()
      .addCredentialsProvider(defaultProvider)
      .addCredentialsProvider(profileProvider)
      .build()
  }
}
