package models

import org.scalatest.{FreeSpec, Matchers}
import util.Fixtures._

class InstanceTest extends FreeSpec with Matchers {

  val i1 = emptyInstance("i1").copy(stack = Some("stack1"), stage = Some("stage1"), app = List("app1"))

  "Instance" - {
    "should belong to SSA with same stack name" in {
      val stack = SSA(stack = i1.stack)
      i1.belongsToSSA(stack) should be (true)
    }
    "should belong to SSA with same stack and stage name" in {
      val stack = SSA(stack = i1.stack, stage = i1.stage)
      i1.belongsToSSA(stack) should be (true)
    }
    "should belong to SSA with same stack, stage and app name" in {
      val stack = SSA(stack = i1.stack, stage = i1.stage, app = i1.app.headOption)
      i1.belongsToSSA(stack) should be (true)
    }
    "should not belong to SSA with different stack name" in {
      val stack = SSA(stack = Some("another stack"))
      i1.belongsToSSA(stack) should be (false)
    }
    "should not belong to SSA with same stack name but different stage name" in {
      val stack = SSA(stack = i1.stack, stage = Some("another stage"))
      i1.belongsToSSA(stack) should be (false)
    }
    "should not belong to SSA with same stack and stage name but different app name" in {
      val stack = SSA(stack = i1.stack, stage = i1.stack, app = Some("another app"))
      i1.belongsToSSA(stack) should be (false)
    }
    "should not belong to SSA with same stage name but different stack name" in {
      val stack = SSA(stack = Some("another stack"), stage = i1.stage)
      i1.belongsToSSA(stack) should be (false)
    }
  }
}
