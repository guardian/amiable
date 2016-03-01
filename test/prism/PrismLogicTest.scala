package prism

import models.SSA
import org.joda.time.DateTime
import org.scalatest.{FreeSpec, Matchers}


class PrismLogicTest extends FreeSpec with Matchers {
  import prism.PrismLogic._
  import util.Fixtures._

  "oldInstances" - {
    val i1 = emptyInstance("i1")
    val i2 = emptyInstance("i2")
    val youngAmi = emptyAmi("a1").copy(creationDate = Some(DateTime.now.minusDays(1)))
    val oldAmi = emptyAmi("a2").copy(creationDate = Some(DateTime.now.minusDays(40)))
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
    val a1 = emptyAmi("arn-1")
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
    val a1 = emptyAmi("arn-1")
    val a2 = emptyAmi("arn-2")
    val a3 = emptyAmi("arn-not-used")
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

  "instanceSSAs" - {
    val emptySSA = SSA(None, None, None)

    "returns empty list for no instances" in {
      instanceSSAs(Nil) shouldEqual Nil
    }

    "returns the empty SSA when an instance has no SSA fields" in {
      instanceSSAs(List(instanceWithSSA("i1", emptySSA))) shouldEqual List(emptySSA)
    }

    "returns single empty SSA for a collection of instances with no SSA fields" in {
      val instances = List(instanceWithSSA("i1", emptySSA), instanceWithSSA("i2", emptySSA))
      instanceSSAs(instances) shouldEqual List(emptySSA)
    }

    "returns the correct SSA for an instance" in {
      val ssa = SSA(Some("stack"), Some("app"), Some("app"))
      instanceSSAs(List(instanceWithSSA("i1", ssa))) shouldEqual List(ssa)
    }

    "returns the correct SSAs for multiple instances" in {
      val ssa1 = SSA(Some("stack-1"), Some("stage-1"), Some("app-1"))
      val ssa2 = SSA(Some("stack-2"), Some("stage-2"), Some("app-2"))
      instanceSSAs(List(instanceWithSSA("i1", ssa1), instanceWithSSA("i2", ssa2))) shouldEqual List(ssa1, ssa2)
    }
  }

  "amiSSAs" - {
    val a1 = emptyAmi("a1")
    val a2 = emptyAmi("a2")

    "associates" - {
      val ssa1 = SSA(Some("stack-1"), Some("stage-1"), Some("app-1"))
      val ssa2 = SSA(Some("stack-2"), Some("stage-2"), Some("app-2"))
      val i1 = instanceWithSSA("i1", ssa1)
      val i2 = instanceWithSSA("i2", ssa2)

      "an SSA with an AMI" in {
        amiSSAs(List(a1 -> List(i1))) shouldEqual Map(ssa1 -> List(a1))
      }

      "an SSA with multiple AMIs" in {
        amiSSAs(List(a1 -> List(i1), a2 -> List(i1))) shouldEqual Map(ssa1 -> List(a1, a2))
      }

      "an AMI with multiple SSAs when it's used on different instances" in {
        amiSSAs(List(a1 -> List(i1, i2))) shouldEqual Map(ssa1 -> List(a1), ssa2 -> List(a1))
      }
    }

    "if an instance's app is empty, associates with the stack/stage combo" in {
      val stackStage = SSA(Some("stack"), Some("stage"), None)
      val instance = instanceWithSSA("i", stackStage)
      amiSSAs(List(a1 -> List(instance))) shouldEqual Map(stackStage -> List(a1))
    }

    "correctly associates instances with multiple apps" in {
      val instance = emptyInstance("i").copy(app = List("app1", "app2"))
      amiSSAs(List(a1 -> List(instance))) shouldEqual Map(
        SSA(None, None, Some("app1")) -> List(a1),
        SSA(None, None, Some("app2")) -> List(a1)
      )
    }

    "combines multi-app instances with other instances" in {
      val i1 = emptyInstance("i1").copy(app = List("app1", "another-app"))
      val i2 = emptyInstance("i2").copy(app = List("app1", "app2"))
      val i3 = emptyInstance("i3").copy(app = List("app2"))
      amiSSAs(List(a1 -> List(i1, i2), a2 -> List(i3))) shouldEqual Map(
        SSA(None, None, Some("app1"))-> List(a1),
        SSA(None, None, Some("another-app"))-> List(a1),
        SSA(None, None, Some("app2")) -> List(a1, a2)
      )
    }
  }

  "sortSSAAmisByAge" - {
    val ssa1 = SSA(Some("stack-1"))
    val ssa2 = SSA(Some("stack-2"))
    val ssaEmpty = SSA()
    val oldAmi = emptyAmi("old").copy(creationDate = Some(DateTime.now.minusDays(40)))
    val newAmi = emptyAmi("new").copy(creationDate = Some(DateTime.now.minusDays(1)))
    val mediumAmi = emptyAmi("medium").copy(creationDate = Some(DateTime.now.minusDays(20)))

    "puts an SSA group with an old AMI before one with a newer AMI" in {
      sortSSAAmisByAge(Map(ssa1 -> List(oldAmi), ssa2 -> List(newAmi))).map(_._1) should contain inOrderOnly(ssa1, ssa2)
    }

    "sorts by oldest AMI (SSA with old AMI goes first, even if it has a new AMI as well)" in {
      sortSSAAmisByAge(Map(ssa1 -> List(oldAmi, newAmi), ssa2 -> List(mediumAmi))).map(_._1) should contain inOrderOnly(ssa1, ssa2)
    }

    "empty SSA goes last even if it has the oldest AMIs" in {
      sortSSAAmisByAge(Map(ssaEmpty -> List(oldAmi), ssa1 -> List(newAmi))).map(_._1) should contain inOrderOnly(ssa1, ssaEmpty)
    }
  }

  "amiIsOld" - {
    "returns false for a fresh AMI" in {
      val a1 = emptyAmi("a1").copy(creationDate = Some(DateTime.now.minusDays(1)))
      amiIsOld(a1) shouldEqual false
    }

    "returns true for an ageing AMI" in {
      val a1 = emptyAmi("a1").copy(creationDate = Some(DateTime.now.minusDays(40)))
      amiIsOld(a1) shouldEqual true
    }
  }
}
