package services.notification

import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain}
import com.amazonaws.auth.profile.ProfileCredentialsProvider

object AwsCredentials {
  val provider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("deployTools"),
    new DefaultAWSCredentialsProviderChain()
  )
}
