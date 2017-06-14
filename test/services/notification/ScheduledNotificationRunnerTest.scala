package services.notification

import com.amazonaws.services.simpleemail.model.SendEmailRequest
import config.AMIableConfig
import models._
import org.scalatest.{EitherValues, FreeSpec, Matchers}
import play.api.Mode
import util.{AttemptValues, Fixtures}
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar

import scala.concurrent.ExecutionContext.Implicits.global

class ScheduledNotificationRunnerTest extends FreeSpec with Matchers with AttemptValues with EitherValues with MockitoSugar {

  val defaultOwner = Owner("Director of Engineering", List.empty)

  "ownerForInstance should return the owner of an instance" in {
    val discussion = Owner("discussiondev", List(SSA(Some("discussion"), Some("PROD"), Some("api"))))
    val capi = Owner("capi", List(SSA(Some("capi"), Some("PROD"))))
    val owners = List(discussion, capi)

    val discussionApiProd = Fixtures.instanceWithSSA("arn3", SSA(Some("discussion"), Some("PROD"), Some("api")))

    ScheduledNotificationRunner.ownerForInstance(discussionApiProd, Owners(owners, defaultOwner)) should be(discussion)
  }

  "ownerForInstance should return the most specific owner of an instance" in {
    val amigoOwner = Owner("amigoOwner", List(SSA(Some("devtools"), app = Some("amigo"))))
    val amigoProdOwner = Owner("amigoProdOwner", List(SSA(Some("devtools"), Some("PROD"), Some("amigo"))))
    val owners = List(amigoOwner, amigoProdOwner)

    val amigoProd = Fixtures.instanceWithSSA("arn3", SSA(Some("devtools"), Some("PROD"), Some("amigo")))

    ScheduledNotificationRunner.ownerForInstance(amigoProd, Owners(owners, defaultOwner)) should be(amigoProdOwner)
  }

  "ownerForInstance should return the most specific owner of an instance 2" in {
    val amigoOwner = Owner("devtools", List(SSA(Some("devtools"), app = Some("amigo"))))
    val devtoolsProd = Owner("capi", List(SSA(Some("devtools"), Some("PROD"))))
    val owners = List(amigoOwner, devtoolsProd)

    val amigoProd = Fixtures.instanceWithSSA("arn3", SSA(Some("devtools"), Some("PROD"), Some("amigo")))

    ScheduledNotificationRunner.ownerForInstance(amigoProd, Owners(owners, defaultOwner)) should be(amigoOwner)
  }

  "ownerForInstance should return the default Owner if no other owner is found" in {
    val amigoOwner = Owner("devtools", List(SSA(Some("devtools"), Some("amigo"))))
    val devtoolsProd = Owner("capi", List(SSA(Some("devtools"), Some("PROD"))))
    val owners = List(amigoOwner, devtoolsProd)

    val capiProd = Fixtures.instanceWithSSA("arn3", SSA(Some("capi"), Some("PROD")))

    ScheduledNotificationRunner.ownerForInstance(capiProd, Owners(owners, defaultOwner)) should be(defaultOwner)
  }

  "createEmailRequest should use the override To address if it is defined" in {
    val owner = Owner("john.doe", List.empty)
    val config = AMIableConfig("prismUrl", null, "fromAddress", None, overrideToAddress = Some("admin@guardian.co.uk"))
    val req = ScheduledNotificationRunner.createEmailRequest(owner, List.empty, config)
    req.getDestination.getToAddresses.get(0) should be("admin@guardian.co.uk")
  }

  "createEmailRequest should use the owner's id in the To address if the override address is not defined" in {
    val owner = Owner("john.doe", List.empty)
    val config = AMIableConfig("prismUrl", null, "fromAddress", None, overrideToAddress = None)
    val req = ScheduledNotificationRunner.createEmailRequest(owner, List.empty, config)
    req.getDestination.getToAddresses.get(0) should be("john.doe@guardian.co.uk")
  }

  "conditionallySendEmail should send email if we are in Prod" in {
    val mailClient = mock[AWSMailClient]
    val request = mock[SendEmailRequest]
    val owner = Owner("ownerId", List.empty)
    when(mailClient.send("ownerId@guardian.co.uk", request)).thenReturn(Attempt.Right("MessageSentId"))
    val res = ScheduledNotificationRunner.conditionallySendEmail(Mode.Prod, None, mailClient, owner, request)
    res.awaitEither.right.value shouldBe "MessageSentId"
  }

  "conditionallySendEmail should send email if the override address is set" in {
    val mailClient = mock[AWSMailClient]
    val request = mock[SendEmailRequest]
    val owner = Owner("ownerId", List.empty)
    when(mailClient.send("overrideToaddress", request)).thenReturn(Attempt.Right("MessageSentId"))
    val res = ScheduledNotificationRunner.conditionallySendEmail(Mode.Dev, Some("overrideToaddress"), mailClient, owner, request)
    res.awaitEither.right.value shouldBe "MessageSentId"
  }

  "conditionallySendEmail should not send email if the override address is not set and we are not in Prod" in {
    val mailClient = mock[AWSMailClient]
    val request = mock[SendEmailRequest]
    val owner = Owner("ownerId", List.empty)
    when(mailClient.send("overrideToaddress", request)).thenReturn(Attempt.Right("MessageSentId"))
    val res = ScheduledNotificationRunner.conditionallySendEmail(Mode.Dev, None, mailClient, owner, request)
    res.awaitEither.right.value shouldBe ""
    verify(mailClient, never()).send(anyString(), anyObject())
  }
}
