package prism

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, Matchers}


class PrismLogicTest extends FreeSpec with Matchers {
  import prism.PrismLogic._
  import util.Fixtures._

  "oldInstances" - {
    val i1 = emptyInstance("i1")
    val i2 = emptyInstance("i2")
    val youngAmi = emptyAmi.copy(creationDate = Some(DateTime.now.minusDays(1)))
    val oldAmi = emptyAmi.copy(creationDate = Some(DateTime.now.minusDays(40)))
    val instances = List(i1 -> Some(youngAmi), i2 -> Some(oldAmi))

    "returns old instances" in {
      oldInstances(instances) should contain(i2)
    }

    "excludes young instances" in {
      oldInstances(instances) shouldNot contain(i1)
    }
  }

  "stacks" - {
    "dedupes stacks" in {
      val i1 = emptyInstance("i1").copy(stack = Some("stack"))
      val i2 = emptyInstance("i2").copy(stack = Some("stack"))
      stacks(List(i1, i2)) shouldEqual List("stack")
    }

    "returns the correct stacks for these instances" in {
      val i1 = emptyInstance("i1").copy(stack = Some("stack1"))
      val i2 = emptyInstance("i2").copy(stack = Some("stack2"))
      val i3 = emptyInstance("i3").copy(stack = Some("stack2"))
      val i4 = emptyInstance("i4").copy(stack = Some("stack3"))
      val i5 = emptyInstance("i5").copy(stack = None)
      stacks(List(i1, i2, i3, i4, i5)) shouldEqual List("stack1", "stack2", "stack3")
    }
  }

  "amiArns" - {
    val i1 = instanceWithAmiArn("i1", Some("arn-1"))
    val i2 = instanceWithAmiArn("i2", Some("arn-1"))
    val i3 = instanceWithAmiArn("i3", Some("arn-2"))
    val i4 = instanceWithAmiArn("i4", Some("arn-3"))
    val i5 = instanceWithAmiArn("i5", None)

    "dedupes ARNs" in {
      amiArns(List(i1, i2)) shouldEqual List("arn-1")
    }

    "returns the correct ARNs for these instances" in {
      amiArns(List(i1, i2, i3, i4, i5)) shouldEqual List("arn-1", "arn-2", "arn-3")
    }
  }

  "instanceAmis" - {
    val a1 = emptyAmi.copy(arn = "arn-1")
    val i1 = instanceWithAmiArn("i1", Some("arn-1"))
    val i2 = instanceWithAmiArn("i2", Some("arn-1"))

    "associates an instance with its AMI" in {
      instanceAmis(List(i1), List(a1)) shouldEqual List(i1 -> Some(a1))
    }

    "associates instance with None if its AMI is not present" in {
      instanceAmis(List(i1), Nil) shouldEqual List(i1 -> None)
    }

    "associates multiple instances with a shared AMI" in {
      instanceAmis(List(i1, i2), List(a1)) shouldEqual List(i1 -> Some(a1), i2 -> Some(a1))
    }
  }

  "amiInstances" - {
    val a1 = emptyAmi.copy(arn = "arn-1")
    val a2 = emptyAmi.copy(arn = "arn-2")
    val a3 = emptyAmi.copy(arn = "arn-not-used")
    val i1 = instanceWithAmiArn("i1", Some("arn-1"))
    val i2 = instanceWithAmiArn("i2", Some("arn-2"))
    val i3 = instanceWithAmiArn("i3", Some("arn-1"))
    val i4 = instanceWithAmiArn("i4", None)

    "associates an ami with its instances" in {
      amiInstances(List(a1), List(i1)) shouldEqual List(a1 -> List(i1))
    }

    "associates ami with Nil if no instances use it" in {
      amiInstances(List(a3), List(i1, i2, i3, i4)) shouldEqual List(a3 -> Nil)
    }

    "associates ami with Nil if no instances are available" in {
      amiInstances(List(a1), Nil) shouldEqual List(a1 -> Nil)
    }

    "associates an AMI with multiple instances that are based on it" in {
      amiInstances(List(a1), List(i1, i3)) shouldEqual List(a1 -> List(i1, i3))
    }

    "associates various instances correctly" in {
      amiInstances(List(a1, a2, a3), List(i1, i2, i3, i4)) shouldEqual List(a1 -> List(i1, i3), a2 -> List(i2), a3 -> Nil)
    }
  }

  "amiIsOld" - {
    "returns false for a fresh AMI" in {
      val a1 = emptyAmi.copy(creationDate = Some(DateTime.now.minusDays(1)))
      amiIsOld(a1) shouldEqual false
    }

    "returns true for an ageing AMI" in {
      val a1 = emptyAmi.copy(creationDate = Some(DateTime.now.minusDays(40)))
      amiIsOld(a1) shouldEqual true
    }
  }
}
