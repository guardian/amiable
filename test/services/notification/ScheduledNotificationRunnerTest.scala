package services.notification

import models.{Owner, SSA}
import org.scalatest.{FreeSpec, Matchers}
import util.Fixtures

class ScheduledNotificationRunnerTest extends FreeSpec with Matchers {

  "infoForOwner should extract information about stacks that are owned by the specific owner" in {
      val owner = Owner ("discussiondev", List(SSA(Some("capi"), Some("PROD")), SSA(Some("discussion"), Some("PROD"), Some("api"))))

      val instances = List(
        Fixtures.instanceWithSSA("arn1", SSA(Some("frontend"))),
        Fixtures.instanceWithSSA("arn2", SSA(Some("capi"), Some("PROD"))),
        Fixtures.instanceWithSSA ("arn3", SSA(Some("discussion"), Some("PROD"), Some("api")))
      )
      ScheduledNotificationRunner.infoForOwner(owner, instances).address should startWith (owner.id)
      ScheduledNotificationRunner.infoForOwner(owner, instances).message should (include("arn2") and include("arn3"))
  }
}
