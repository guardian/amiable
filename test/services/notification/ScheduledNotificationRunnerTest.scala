package services.notification

import models.{Owner, SSA}
import org.scalatest.{FreeSpec, Matchers}
import util.Fixtures

class ScheduledNotificationRunnerTest extends FreeSpec with Matchers {

  "infoForOwner should extract the stacks that are owned by the specific owner" in {
      val owner = Owner ("discussiondev", List(SSA(Some("capi"), Some("PROD")), SSA(Some("discussion"), Some("PROD"), Some("api"))))

    val frontend = Fixtures.instanceWithSSA("arn1", SSA(Some("frontend")))
    val capiProd = Fixtures.instanceWithSSA("arn2", SSA(Some("capi"), Some("PROD")))
    val discussionApiProd = Fixtures.instanceWithSSA("arn3", SSA(Some("discussion"), Some("PROD"), Some("api")))

    val instances = List(
        frontend,
        capiProd,
        discussionApiProd
      )
      ScheduledNotificationRunner.instancesForOwner(owner, instances) should contain theSameElementsAs List(capiProd, discussionApiProd)
  }

  "infoForOwner should extract the instances that belong to a specific stack" in {
    val owner = Owner ("discussiondev", List(SSA(Some("discussion"))))

    val discussionModtools = Fixtures.instanceWithSSA("arn1", SSA(Some("discussion"), app = Some("modtools")))
    val capiProd = Fixtures.instanceWithSSA("arn2", SSA(Some("capi"), Some("PROD")))
    val discussionApiProd = Fixtures.instanceWithSSA("arn3", SSA(Some("discussion"), Some("PROD"), Some("api")))

    val instances = List(
      discussionModtools,
      capiProd,
      discussionApiProd
    )
    ScheduledNotificationRunner.instancesForOwner(owner, instances) should contain theSameElementsAs List(discussionModtools, discussionApiProd)
  }
}
