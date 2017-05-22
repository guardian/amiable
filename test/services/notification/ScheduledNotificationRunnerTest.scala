package services.notification

import models.{Owner, Owners, SSA}
import org.scalatest.{FreeSpec, Matchers}
import util.Fixtures

class ScheduledNotificationRunnerTest extends FreeSpec with Matchers {

  val defaultOwner = Owner("Director of Engineering", List.empty)

  "ownerForInstance should return the owner of an instance" in {
    val discussion = Owner ("discussiondev", List(SSA(Some("discussion"), Some("PROD"), Some("api"))))
    val capi = Owner ("capi", List(SSA(Some("capi"), Some("PROD"))))
    val owners = List (discussion, capi)

    val discussionApiProd = Fixtures.instanceWithSSA("arn3", SSA(Some("discussion"), Some("PROD"), Some("api")))

    ScheduledNotificationRunner.ownerForInstance(discussionApiProd, Owners(owners ,defaultOwner)) should be (discussion)
  }

  "ownerForInstance should return the most specific owner of an instance" in {
    val amigoOwner = Owner ("amigoOwner", List(SSA(Some("devtools"), app = Some("amigo"))))
    val amigoProdOwner = Owner ("amigoProdOwner", List(SSA(Some("devtools"), Some("PROD"), Some("amigo"))))
    val owners = List (amigoOwner, amigoProdOwner)

    val amigoProd = Fixtures.instanceWithSSA("arn3", SSA(Some("devtools"), Some("PROD"), Some("amigo")))

    ScheduledNotificationRunner.ownerForInstance(amigoProd, Owners(owners ,defaultOwner)) should be (amigoProdOwner)
  }

  "ownerForInstance should return the most specific owner of an instance 2" in {
    val amigoOwner = Owner ("devtools", List(SSA(Some("devtools"),app = Some("amigo"))))
    val devtoolsProd = Owner ("capi", List(SSA(Some("devtools"), Some("PROD"))))
    val owners = List (amigoOwner, devtoolsProd)

    val amigoProd = Fixtures.instanceWithSSA("arn3", SSA(Some("devtools"), Some("PROD"), Some("amigo")))

    ScheduledNotificationRunner.ownerForInstance(amigoProd, Owners(owners ,defaultOwner)) should be (amigoOwner)
  }

  "ownerForInstance should return the default Owner if no other owner is found" in {
    val amigoOwner = Owner ("devtools", List(SSA(Some("devtools"), Some("amigo"))))
    val devtoolsProd = Owner ("capi", List(SSA(Some("devtools"), Some("PROD"))))
    val owners = List (amigoOwner, devtoolsProd)

    val capiProd = Fixtures.instanceWithSSA("arn3", SSA(Some("capi"), Some("PROD")))

    ScheduledNotificationRunner.ownerForInstance(capiProd, Owners(owners ,defaultOwner)) should be (defaultOwner)
  }
}
