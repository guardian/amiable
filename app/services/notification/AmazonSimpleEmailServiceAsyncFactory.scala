package services.notification

import javax.inject.Inject

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail._
import com.google.inject.Provider


class AmazonSimpleEmailServiceAsyncFactory @Inject()(awsCredentials :AwsCredentials) extends Provider[AmazonSimpleEmailServiceAsync]{

  override def get(): AmazonSimpleEmailServiceAsync = {
    AmazonSimpleEmailServiceAsyncClientBuilder.standard()
      .withRegion(Regions.EU_WEST_1)
      .withCredentials(awsCredentials.provider)
    .build()
  }
}
