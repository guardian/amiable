package services.notification

import javax.inject.Singleton

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain}

@Singleton
class AwsCredentials {
  val provider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("deployTools"),
    new DefaultAWSCredentialsProviderChain()
  )
}
