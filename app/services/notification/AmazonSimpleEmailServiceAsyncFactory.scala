package services.notification

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail._
import com.google.inject.Provider


class AmazonSimpleEmailServiceAsyncFactory extends Provider[AmazonSimpleEmailServiceAsync]{

  override def get(): AmazonSimpleEmailServiceAsync = {
    AmazonSimpleEmailServiceAsyncClientBuilder.standard()
      .withRegion(Regions.EU_WEST_1)
      .withCredentials(AwsCredentials.provider)
    .build()
  }
}
