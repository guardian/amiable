package prism

import datetime.DateUtils
import models._
import org.joda.time.DateTime

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
    * T will either be an AMI, or an AMI attempt.
    * From a full list of Ts and instances, return each unique
    * SSA combination with all its associated Ts.
    */
  def amiSSAs[T](amisWithInstances: List[(T, List[Instance])]): Map[SSA, List[T]] = {
    val allSSACombos = for {
      (t, instances) <- amisWithInstances
      ssa <- instanceSSAs(instances)
    } yield ssa -> t

    allSSACombos
      .groupBy { case (ssa, _) => ssa }
      .map { case (ssa, ssaAmis) =>
        ssa -> ssaAmis.map { case (_, t) => t }
      }
  }

  /**
    * SSAs are sorted by their oldest AMI, except for the empty SSA which
    * always appears last.
    */
  def sortSSAAmisByAge(ssaAmis: Map[SSA, List[AMI]]): List[(SSA, List[AMI])] = {
    ssaAmis.toList.sortBy { case (ssa, amis) =>
      if (ssa.isEmpty) {
        // put empty SSA last
        DateTime.now.getMillis
      } else {
        val creationDates = for {
          ami <- amis
          creationDate <- ami.creationDate
        } yield creationDate.getMillis
        creationDates.headOption.getOrElse(0L)
      }
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
