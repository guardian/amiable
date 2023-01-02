package prism

import models._
import org.joda.time.DateTime
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import scala.util.Random

class PrismLogicTest extends AnyFreeSpec with Matchers {

  import prism.PrismLogic._
  import util.Fixtures._

  "oldInstances" - {
    val i1 = emptyInstance("i1")
    val i2 = emptyInstance("i2")
    val youngAmi =
      emptyAmi("a1").copy(creationDate = Some(DateTime.now.minusDays(1)))
    val oldAmi =
      emptyAmi("a2").copy(creationDate = Some(DateTime.now.minusDays(40)))
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
      stacks(List(i1, i2, i3, i4, i5)) shouldEqual List(
        "stack1",
        "stack2",
        "stack3"
      )
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
      amiArns(List(i1, i2, i3, i4, i5)) shouldEqual List(
        "arn-1",
        "arn-2",
        "arn-3"
      )
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
      instanceAmis(List(i1, i2), List(a1)) shouldEqual List(
        i1 -> Some(a1),
        i2 -> Some(a1)
      )
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
      amiInstances(List(a1, a2, a3), List(i1, i2, i3, i4)) shouldEqual List(
        a1 -> List(i1, i3),
        a2 -> List(i2),
        a3 -> Nil
      )
    }
  }

  "instanceSSAAs" - {
    val emptySSAA = SSAA(None, None, None, None)

    "returns empty list for no instances" in {
      instanceSSAAs(Nil) shouldEqual Nil
    }

    "returns the empty SSA when an instance has no SSA fields" in {
      instanceSSAAs(List(instanceWithSSAA("i1", emptySSAA))) shouldEqual List(
        emptySSAA
      )
    }

    "returns single empty SSA for a collection of instances with no SSA fields" in {
      val instances = List(
        instanceWithSSAA("i1", emptySSAA),
        instanceWithSSAA("i2", emptySSAA)
      )
      instanceSSAAs(instances) shouldEqual List(emptySSAA)
    }

    "returns the correct SSA for an instance" in {
      val ssaa = SSAA(Some("stack"), Some("app"), Some("app"), Some("account"))
      instanceSSAAs(List(instanceWithSSAA("i1", ssaa))) shouldEqual List(ssaa)
    }

    "returns the correct SSAs for multiple instances" in {
      val ssa1 =
        SSAA(Some("stack-1"), Some("stage-1"), Some("app-1"), Some("acc-1"))
      val ssa2 =
        SSAA(Some("stack-2"), Some("stage-2"), Some("app-2"), Some("acc-2"))
      instanceSSAAs(
        List(instanceWithSSAA("i1", ssa1), instanceWithSSAA("i2", ssa2))
      ) shouldEqual List(ssa1, ssa2)
    }
  }

  "amiSSAs" - {
    val a1 = emptyAmi("a1")
    val a2 = emptyAmi("a2")

    "associates" - {
      val ssa1 =
        SSAA(Some("stack-1"), Some("stage-1"), Some("app-1"), Some("acc-1"))
      val ssa2 =
        SSAA(Some("stack-2"), Some("stage-2"), Some("app-2"), Some("acc-2"))
      val i1 = instanceWithSSAA("i1", ssa1)
      val i2 = instanceWithSSAA("i2", ssa2)

      "an SSA with an AMI" in {
        amiSSAAs(List(a1 -> List(i1))) shouldEqual Map(ssa1 -> List(a1))
      }

      "an SSA with multiple AMIs" in {
        amiSSAAs(List(a1 -> List(i1), a2 -> List(i1))) shouldEqual Map(
          ssa1 -> List(a1, a2)
        )
      }

      "an AMI with multiple SSAs when it's used on different instances" in {
        amiSSAAs(List(a1 -> List(i1, i2))) shouldEqual Map(
          ssa1 -> List(a1),
          ssa2 -> List(a1)
        )
      }
    }

    "if an instance's app is empty, associates with the stack/stage combo" in {
      val stackStage = SSAA(Some("stack"), Some("stage"), None, Some("acc-1"))
      val instance = instanceWithSSAA("i", stackStage)
      amiSSAAs(List(a1 -> List(instance))) shouldEqual Map(
        stackStage -> List(a1)
      )
    }

    "correctly associates instances with multiple apps" in {
      val instance = emptyInstance("i").copy(
        app = List("app1", "app2"),
        meta = Meta("", Origin("", None, "", ""))
      )
      amiSSAAs(List(a1 -> List(instance))) shouldEqual Map(
        SSAA(None, None, Some("app1")) -> List(a1),
        SSAA(None, None, Some("app2")) -> List(a1)
      )
    }

    "combines multi-app instances with other instances" in {
      val i1 = emptyInstance("i1").copy(app = List("app1", "another-app"))
      val i2 = emptyInstance("i2").copy(app = List("app1", "app2"))
      val i3 = emptyInstance("i3").copy(app = List("app2"))
      amiSSAAs(List(a1 -> List(i1, i2), a2 -> List(i3))) shouldEqual Map(
        SSAA(None, None, Some("app1")) -> List(a1),
        SSAA(None, None, Some("another-app")) -> List(a1),
        SSAA(None, None, Some("app2")) -> List(a1, a2)
      )
    }
  }

  "sortSSAAmisByAge" - {
    val ssa1 = SSAA(Some("stack-1"))
    val ssa2 = SSAA(Some("stack-2"))
    val ssaEmpty = SSAA()
    val oldAmi =
      emptyAmi("old").copy(creationDate = Some(DateTime.now.minusDays(40)))
    val newAmi =
      emptyAmi("new").copy(creationDate = Some(DateTime.now.minusDays(1)))
    val mediumAmi =
      emptyAmi("medium").copy(creationDate = Some(DateTime.now.minusDays(20)))

    "puts an SSA group with an old AMI before one with a newer AMI" in {
      sortSSAAmisByAge(Map(ssa1 -> List(oldAmi), ssa2 -> List(newAmi)))
        .map(_._1) should contain inOrderOnly (ssa1, ssa2)
    }

    "sorts by oldest AMI (SSA with old AMI goes first, even if it has a new AMI as well)" in {
      sortSSAAmisByAge(
        Map(ssa1 -> List(oldAmi, newAmi), ssa2 -> List(mediumAmi))
      ).map(_._1) should contain inOrderOnly (ssa1, ssa2)
    }

    "empty SSA goes last even if it has the oldest AMIs" in {
      sortSSAAmisByAge(Map(ssaEmpty -> List(oldAmi), ssa1 -> List(newAmi)))
        .map(_._1) should contain inOrderOnly (ssa1, ssaEmpty)
    }
  }

  "amiIsOld" - {
    "returns false for a fresh AMI" in {
      val a1 =
        emptyAmi("a1").copy(creationDate = Some(DateTime.now.minusDays(1)))
      amiIsOld(a1) shouldEqual false
    }

    "returns true for an ageing AMI" in {
      val a1 =
        emptyAmi("a1").copy(creationDate = Some(DateTime.now.minusDays(40)))
      amiIsOld(a1) shouldEqual true
    }
  }

  "instancesAmisAgePercentiles" - {
    val now = DateTime.now

    sealed trait AmiType
    case object AmiWithCreationDate extends AmiType
    case object AmiWithoutCreationDate extends AmiType
    case object NoAmi extends AmiType

    def instancesGen(count: Int, expectedAmiType: AmiType) = Random
      .shuffle(0 to count)
      .toList
      .map { i =>
        val instance: Instance = emptyInstance(s"instance-$i")
        val ami: Option[AMI] = expectedAmiType match {
          case AmiWithCreationDate =>
            Some(
              emptyAmi(s"ami-$i").copy(creationDate = Some(now.minusDays(i)))
            )
          case AmiWithoutCreationDate => Some(emptyAmi(s"ami-$i"))
          case NoAmi                  => None
        }
        (instance, ami)
      }

    "return correct percentiles values when all instances have an AMI with creation date" in {
      val percentiles = instancesAmisAgePercentiles(
        instancesGen(count = 100, expectedAmiType = AmiWithCreationDate)
      )
      percentiles.p25 shouldEqual Some(25)
      percentiles.p50 shouldEqual Some(50)
      percentiles.p75 shouldEqual Some(75)
      percentiles.p90 shouldEqual Some(90)
    }

    "ignore instances with no AMI" in {
      val hasAMICount = 8
      val allInstances = instancesGen(
        count = hasAMICount,
        expectedAmiType = AmiWithCreationDate
      ) ++ instancesGen(count = 100 - hasAMICount, expectedAmiType = NoAmi)
      val percentiles = instancesAmisAgePercentiles(allInstances)
      percentiles.p25 shouldEqual Some(2)
      percentiles.p50 shouldEqual Some(4)
      percentiles.p75 shouldEqual Some(6)
      percentiles.p90 shouldEqual Some(7)
    }

    "ignore instances with AMI that doesn't have a creation date" in {
      val hasAMICount = 16
      val allInstances = instancesGen(
        count = hasAMICount,
        expectedAmiType = AmiWithCreationDate
      ) ++ instancesGen(
        count = 100 - hasAMICount,
        expectedAmiType = AmiWithoutCreationDate
      )
      val percentiles = instancesAmisAgePercentiles(allInstances)
      percentiles.p25 shouldEqual Some(4)
      percentiles.p50 shouldEqual Some(8)
      percentiles.p75 shouldEqual Some(12)
      percentiles.p90 shouldEqual Some(14)
    }
  }

  "instancesCountPerSsaPerAmi" - {
    "correctly associates the instance count with each couple SSA/AMI" in {
      val ssa1 = SSAA(Some("stack-1"), Some("stage-1"), Some("app-1"))
      val ssa2 = SSAA(Some("stack-2"), Some("stage-2"))
      val ssa3 = SSAA(Some("stack-3"))
      val a1 = emptyAmi("a1")
      val a2 = emptyAmi("a2")
      val i1 = emptyInstance("i1").copy(
        stack = Some("stack-1"),
        stage = Some("stage-1"),
        app = List("app-1")
      )
      val i2 = emptyInstance("i2").copy(
        stack = Some("stack-2"),
        stage = Some("stage-2"),
        app = List("app-2")
      )
      val i3 = emptyInstance("i3").copy(
        stack = Some("stack-3"),
        stage = Some("stage-2"),
        app = List("app-3")
      )
      val i4 = emptyInstance("i4").copy(
        stack = Some("stack-3"),
        stage = Some("stage-2"),
        app = List("app-4")
      )
      val i5 = emptyInstance("i5").copy(
        stack = Some("stack-1"),
        stage = Some("stage-1"),
        app = List("app-5")
      )
      val i6 = emptyInstance("i6").copy(
        stack = Some("stack-3"),
        stage = Some("stage-2"),
        app = List("app-6")
      )
      val amisWithInstances = List(
        (a1, List(i1, i2, i3)),
        (a2, List(i4, i5, i6))
      )
      val allSSAs = List(ssa1, ssa2, ssa3)
      val expectedResult = Map(
        (ssa1, a1) -> 1,
        (ssa2, a1) -> 1,
        (ssa3, a1) -> 1,
        (ssa3, a2) -> 2
      )
      instancesCountPerSsaPerAmi(amisWithInstances, allSSAs) should be(
        expectedResult
      )
    }
  }

  "doesInstanceBelongToSSA" - {
    val i1 = emptyInstance("i1").copy(
      stack = Some("stack1"),
      stage = Some("stage1"),
      app = List("app1")
    )
    "should return true when instance and SSA have" - {
      "the same stack" in {
        val stack = SSAA(stack = i1.stack)
        doesInstanceBelongToSSA(i1, stack) should be(true)
      }
      "the same stack and stage" in {
        val stack = SSAA(stack = i1.stack, stage = i1.stage)
        doesInstanceBelongToSSA(i1, stack) should be(true)
      }
      "the same stack, stage and app" in {
        val stack =
          SSAA(stack = i1.stack, stage = i1.stage, app = i1.app.headOption)
        doesInstanceBelongToSSA(i1, stack) should be(true)
      }
    }
    "should return false when instance and SSA have" - {
      "different stack" in {
        val stack = SSAA(stack = Some("another stack"))
        doesInstanceBelongToSSA(i1, stack) should be(false)
      }
      "the same stack but different stage" in {
        val stack = SSAA(stack = i1.stack, stage = Some("another stage"))
        doesInstanceBelongToSSA(i1, stack) should be(false)
      }
      "the same stack and stage but different app" in {
        val stack =
          SSAA(stack = i1.stack, stage = i1.stack, app = Some("another app"))
        doesInstanceBelongToSSA(i1, stack) should be(false)
      }
      "the same stage but different stack" in {
        val stack = SSAA(stack = Some("another stack"), stage = i1.stage)
        doesInstanceBelongToSSA(i1, stack) should be(false)
      }
    }
  }

  "sortInstancesByStack" - {
    "sorts by stack, app, stage when stack and stage are non empty" in {
      val in1 = Instance(
        "arn",
        "name",
        "state",
        "group",
        "dns",
        "ip",
        DateTime.now(),
        "instanceName",
        "region",
        "vendor",
        List.empty,
        Map.empty,
        Some("stack1"),
        Some("stageB"),
        List("appB"),
        List.empty,
        Map.empty,
        null
      )
      val in2 = in1.copy(
        stack = Some("stack2"),
        stage = Some("stageA"),
        app = List("appA")
      )
      val instanceList = List(in2, in1)
      PrismLogic.sortInstancesByStack(
        instanceList
      ) should contain inOrderOnly (in1, in2)
    }
    "sorts by stack, app, stage when stack is empty" in {
      val in1 = Instance(
        "arn",
        "name",
        "state",
        "group",
        "dns",
        "ip",
        DateTime.now(),
        "instanceName",
        "region",
        "vendor",
        List.empty,
        Map.empty,
        None,
        Some("stageB"),
        List("app1"),
        List.empty,
        Map.empty,
        null
      )
      val in2 = in1.copy(stage = Some("stageA"), app = List("app2"))
      val instanceList = List(in2, in1)
      PrismLogic.sortInstancesByStack(
        instanceList
      ) should contain inOrderOnly (in1, in2)
    }
    "sorts by stack, app, stage when stack and stage are empty" in {
      val in1 = Instance(
        "arn",
        "name",
        "state",
        "group",
        "dns",
        "ip",
        DateTime.now(),
        "instanceName",
        "region",
        "vendor",
        List.empty,
        Map.empty,
        None,
        None,
        List("app1"),
        List.empty,
        Map.empty,
        null
      )
      val in2 = in1.copy(app = List("app2"))
      val instanceList = List(in2, in1)
      PrismLogic.sortInstancesByStack(
        instanceList
      ) should contain inOrderOnly (in1, in2)
    }
  }

  "sortLCsByOwner" - {
    "sorts Launch Configurations by account first" in {
      val meta1 =
        Meta("href", Origin("vendor", Some("account1"), "region", "accountNum"))
      val lc1 = LaunchConfiguration(
        "arn1",
        "nameB",
        "imageId",
        "region",
        DateTime.now(),
        "t1-micro",
        "key",
        List.empty,
        None,
        meta1
      )

      val meta2 =
        meta1.copy(origin = meta1.origin.copy(accountName = Some("account2")))
      val lc2 = lc1.copy(name = "nameA", meta = meta2)

      val lcList = List(lc2, lc1)
      PrismLogic.sortLCsByOwner(lcList) should contain inOrderOnly (lc1, lc2)
    }
    "sorts Launch Configurations by name when accounts are the same" in {
      val meta1 =
        Meta("href", Origin("vendor", Some("account"), "region", "accountNum"))
      val lc1 = models.LaunchConfiguration(
        "arn1",
        "name1",
        "imageId1",
        "region",
        DateTime.now(),
        "t1-micro",
        "key1",
        List.empty,
        None,
        meta1
      )

      val lc2 = lc1.copy(name = "name2")

      val lcList = List(lc2, lc1)
      PrismLogic.sortLCsByOwner(lcList) should contain inOrderOnly (lc1, lc2)
    }
  }
}
