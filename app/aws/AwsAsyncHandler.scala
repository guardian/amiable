package aws

import scala.concurrent.Future
import scala.jdk.FutureConverters._

object AwsAsyncHandler {
  def awsToScala[T](completableFuture: java.util.concurrent.CompletableFuture[T]): Future[T] = {
    completableFuture.asScala
  }
}
