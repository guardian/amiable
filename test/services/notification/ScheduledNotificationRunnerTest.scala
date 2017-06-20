package services.notification

import com.amazonaws.services.simpleemail.model.SendEmailRequest
import config.AMIableConfig
import models._
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{EitherValues, FreeSpec, Matchers}
import org.scalatest.mock.MockitoSugar
import play.api.Mode
import util.{AttemptValues, Fixtures}

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
    val date = new DateTime(2017,6,1,11,0,0)
    val owner = Owner("john.doe", List.empty)
    val config = AMIableConfig("prismUrl", null, "fromAddress", None, overrideToAddress = Some("admin@guardian.co.uk"), "http://test-url")
    val req = ScheduledNotificationRunner.createEmailRequest(owner, List.empty, config, date)
    req.getDestination.getToAddresses.get(0) should be("admin@guardian.co.uk")
  }

  "createEmailRequest should use the date in the subject line" in {
    val date = new DateTime(2017,6,1,11,0,0)
    val owner = Owner("john.doe", List.empty)
    val config = AMIableConfig("prismUrl", null, "fromAddress", None, overrideToAddress = None, "http://test-url")
    val req = ScheduledNotificationRunner.createEmailRequest(owner, List.empty, config, date)
    req.getMessage.getSubject.getData should include ("2017-06-01")
  }

  "createEmailRequest should use the owner's id in the To address if the override address is not defined" in {
    val date = new DateTime(2017,6,1,11,0,0)
    val owner = Owner("john.doe", List.empty)
    val config = AMIableConfig("prismUrl", null, "fromAddress", None, overrideToAddress = None, "http://test-url")
    val req = ScheduledNotificationRunner.createEmailRequest(owner, List.empty, config, date)
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

  "pairInstancesWithAmiAge should order unknown ages first and then decreasing age second" in {
    val noAmiAge = Fixtures.emptyInstance("I:no ami age")
    val longAge = Fixtures.emptyInstance("I:long ami age")
    val medAge = Fixtures.emptyInstance("I:shorter ami age")
    val shortAge = Fixtures.emptyInstance("I:short ami age")
    val noAmi = Fixtures.emptyAmi("AMI:no ami age")
    val longAmi = Fixtures.emptyAmi("AMI:long ami age").copy(creationDate = Some(new DateTime().minusDays(500)))
    val medAmi = Fixtures.emptyAmi("AMI:shorter ami age").copy(creationDate = Some(new DateTime().minusDays(100)))
    val shortAmi = Fixtures.emptyAmi("AMI:short ami age").copy(creationDate = Some(new DateTime().minusDays(10)))
    val instances = Map(
      medAge -> medAmi,
      longAge -> longAmi,
      noAmiAge -> noAmi,
      shortAge -> shortAmi
    )
    val results = ScheduledNotificationRunner.pairInstancesWithAmi(instances.keys.toList, instances)
    results shouldBe List(
      noAmiAge -> Some(noAmi),
      longAge -> Some(longAmi),
      medAge -> Some(medAmi),
      shortAge -> Some(shortAmi)
    )
  }
}
