package prism

import models.{AMI, Instance}
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FreeSpec, Matchers, OneInstancePerTest}


class PrismLogicTest extends FreeSpec with Matchers with MockitoSugar with OneInstancePerTest {
  import prism.PrismLogic._

  val (i1, i2, i3, i4, i5) = (mock[Instance], mock[Instance], mock[Instance], mock[Instance], mock[Instance])
  val (a1, a2, a3, a4, a5) = (mock[AMI], mock[AMI], mock[AMI], mock[AMI], mock[AMI])

  "oldInstances" - {
    when(a1.creationDate).thenReturn(Some(DateTime.now.minusDays(1)))
    when(a2.creationDate).thenReturn(Some(DateTime.now.minusDays(40)))

    "returns old instances" in {
      oldInstances(Map(i1 -> Some(a1), i2 -> Some(a2))) should contain(i2)
    }

    "excludes young instances" in {
      oldInstances(Map(i1 -> Some(a1), i2 -> Some(a2))) shouldNot contain(i1)
    }
  }

  "stacks" - {
    "dedupes stacks" in {
      when(i1.stack).thenReturn(Some("stack"))
      when(i2.stack).thenReturn(Some("stack"))
      stacks(List(i1, i2)) shouldEqual List("stack")
    }

    "returns the correct stacks for these instances" in {
      when(i1.stack).thenReturn(Some("stack1"))
      when(i2.stack).thenReturn(Some("stack2"))
      when(i3.stack).thenReturn(Some("stack2"))
      when(i4.stack).thenReturn(Some("stack3"))
      when(i5.stack).thenReturn(None)
      stacks(List(i1, i2, i3, i4, i5)) shouldEqual List("stack1", "stack2", "stack3")
    }
  }

  "amiArns" - {
    when(i1.amiArn).thenReturn(Some("arn-1"))
    when(i2.amiArn).thenReturn(Some("arn-1"))
    when(i3.amiArn).thenReturn(Some("arn-2"))
    when(i4.amiArn).thenReturn(Some("arn-3"))
    when(i5.amiArn).thenReturn(None)

    "dedupes ARNs" in {
      amiArns(List(i1, i2)) shouldEqual List("arn-1")
    }

    "returns the correct ARNs for these instances" in {
      amiArns(List(i1, i2, i3, i4, i5)) shouldEqual List("arn-1", "arn-2", "arn-3")
    }
  }

  "instanceAmis" - {
    when(a1.arn).thenReturn("arn-1")
    when(i1.amiArn).thenReturn(Some("arn-1"))
    when(i2.amiArn).thenReturn(Some("arn-1"))

    "associates an instance with its AMI" in {
      instanceAmis(List(i1), List(a1)) shouldEqual Map(i1 -> Some(a1))
    }

    "associates instance with None if its AMI is not present" in {
      instanceAmis(List(i1), Nil) shouldEqual Map(i1 -> None)
    }

    "associates multiple instances with a shared AMI" in {
      instanceAmis(List(i1, i2), List(a1)) shouldEqual Map(i1 -> Some(a1), i2 -> Some(a1))
    }
  }

  "amiIsOld" - {
    "returns false for a fresh AMI" in {
      when(a1.creationDate).thenReturn(Some(DateTime.now.minusDays(1)))
      amiIsOld(a1) shouldEqual false
    }

    "returns true for an ageing AMI" in {
      when(a1.creationDate).thenReturn(Some(DateTime.now.minusDays(40)))
      amiIsOld(a1) shouldEqual true
    }
  }
}
