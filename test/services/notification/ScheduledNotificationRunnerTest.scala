package services.notification

import models.{Owner, SSA}
import org.scalatest.{FreeSpec, Matchers}
import util.Fixtures

class ScheduledNotificationRunnerTest extends FreeSpec with Matchers {

  "infoForOwner should extract the stacks that are owned by the specific owner" in {
      val owner = Owner ("discussiondev", List(SSA(Some("capi"), Some("PROD")), SSA(Some("discussion"), Some("PROD"), Some("api"))))

    val capiProd = Fixtures.instanceWithSSA("arn2", SSA(Some("capi"), Some("PROD")))
    val discussionApiProd = Fixtures.instanceWithSSA("arn3", SSA(Some("discussion"), Some("PROD"), Some("api")))

    val instances = List(
        Fixtures.instanceWithSSA("arn1", SSA(Some("frontend"))),
        capiProd,
        discussionApiProd
      )
      ScheduledNotificationRunner.instancesForOwner(owner, instances) should contain theSameElementsAs List(capiProd, discussionApiProd)
  }
}
