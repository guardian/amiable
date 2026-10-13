package utils

import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  AwsCredentialsProviderChain,
  DefaultCredentialsProvider,
  ProfileCredentialsProvider
}
import software.amazon.awssdk.regions.Region

object Aws {
  val region: Region = Region.EU_WEST_1

  val credentialsProvider: AwsCredentialsProvider = {

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
