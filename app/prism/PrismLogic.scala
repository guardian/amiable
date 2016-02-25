package prism

import datetime.DateUtils
import models._

object PrismLogic {
  def oldInstances(instanceAmis: List[(Instance, Option[AMI])]): List[Instance] = {
    instanceAmis
      .filter { case (_, amiOpt) => amiOpt.exists(amiIsOld) }
      .map(_._1)
  }

  def stacks(instances: List[Instance]): List[String] = {
    (for {
      instance <- instances
      stack <- instance.stack
    } yield stack).distinct
  }

  def amiArns(instances: List[Instance]): List[String] =
    instances.flatMap(_.amiArn).distinct

  /**
    * Associates instances with their AMI
    */
  def instanceAmis(instances: List[Instance], amis: List[AMI]): List[(Instance, Option[AMI])] = {
    instances.map { instance =>
      instance -> instance.amiArn.flatMap { amiArn =>
        amis.find(_.arn == amiArn)
      }
    }
  }

  /**
    * Associates AMIs with instances that use them
    */
  def amiInstances(amis: List[AMI], instances: List[Instance]): List[(AMI, List[Instance])] = {
    amis.map { ami =>
      ami -> instances.filter { instance =>
        instance.amiArn.fold(false)(_ == ami.arn)
      }
    }
  }

  def instanceSSAs(instances: List[Instance]): List[SSA] = {
    val allInstanceSSAs = for {
      instance <- instances
      ssa <- {
        if (instance.app.isEmpty) List(SSA(instance.stack, instance.stage, None))
        else instance.app.map(app => SSA(instance.stack, instance.stage, Some(app)))
      }
    } yield ssa
    allInstanceSSAs.distinct
  }

  /**
    * From a full list of AMIs and instances, return each unique
    * SSA combination with all its associated AMIs.
    */
  def amiSSAs(amisWithInstances: List[(AMI, List[Instance])]): Map[SSA, List[AMI]] = {
    val allSSACombos = for {
      (ami, instances) <- amisWithInstances
      ssa <- instanceSSAs(instances)
    } yield ssa -> ami

    allSSACombos
      .groupBy { case (ssa, _) => ssa }
      .map { case (ssa, ssaAmis) =>
        ssa -> ssaAmis.map { case (_, ami) => ami }
      }
  }

  def amiIsOld(ami: AMI): Boolean = {
    ami.creationDate.flatMap { creationDate =>
      DateUtils.getAge(creationDate).map {
        case Fresh | Turning => false
        case _ => true
      }
    }.getOrElse(true)
  }
}
