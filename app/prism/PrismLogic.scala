package prism

import models._
import org.joda.time.DateTime
import utils.{DateUtils, Percentiles}

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

  def instanceSSAAs(instances: List[Instance]): List[SSAA] = {
    val allInstanceSSAs = for {
      instance <- instances
      ssa <- {
        if (instance.app.isEmpty) List(SSAA(instance.stack, instance.stage, None, Some(instance.meta.origin.accountName)))
        else instance.app.map(app => SSAA(instance.stack, instance.stage, Some(app), Some(instance.meta.origin.accountName)))
      }
    } yield ssa
    allInstanceSSAs.distinct
  }

  /**
    * T will either be an AMI, or an AMI attempt.
    * From a full list of Ts and instances, return each unique
    * SSA combination with all its associated Ts.
    */
  def amiSSAs[T](amisWithInstances: List[(T, List[Instance])]): Map[SSAA, List[T]] = {
    val allSSACombos = for {
      (t, instances) <- amisWithInstances
      ssa <- instanceSSAAs(instances)
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
  def sortSSAAmisByAge(ssaAmis: Map[SSAA, List[AMI]]): List[(SSAA, List[AMI])] = {
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

  def instancesAmisAgePercentiles(instances: List[(Instance, Option[AMI])]): Percentiles = {
    val ages = instances.flatMap { case (instance, ami) =>
      ami.flatMap(_.creationDate.map(DateUtils.daysAgo))
    }
    Percentiles(ages)
  }

  /**
    * T will either be an AMI, or an AMI attempt.
    * From a full list of Ts and instances and a list of SSAs, return unique
    * SSA and AMI combinations with their respective number of instances
    */
  def instancesCountPerSsaPerAmi[T](amisWithInstances: List[(T, List[Instance])], ssas: List[SSAA]): Map[(SSAA, T), Int] = {
    for {
      (t, instances) <- amisWithInstances.toMap
      ssa <- ssas
      instancesCount = instances.count(i => doesInstanceBelongToSSA(i, ssa))
      if(instancesCount > 0)
    } yield (ssa, t) -> instancesCount
  }

  def doesInstanceBelongToSSA(instance: Instance, ssa: SSAA): Boolean = ssa.stack == instance.stack &&
    ssa.stage.fold(true)(s => instance.stage.contains(s)) &&
    ssa.app.fold(true)(instance.app.contains(_))

  /**
    * Sort Launch Configurations by accountName (ie. the owner of the stack)
    * and by Launch Configuration name, in ascending order
    */
  def sortLCsByOwner(configs: List[LaunchConfiguration]): List[LaunchConfiguration] = {
    configs.sortBy(lc => (lc.meta.origin.accountName, lc.name))
  }

  /**
    * Sorts Instances by Stack, App, Stage in ascending order
    */
  def sortInstancesByStack(instances: List[Instance]): List[Instance] = {
    instances.sortBy(instance => (instance.stack, instance.app.headOption, instance.stage))
  }
}
