package prism

import models.AMI

object Recommendations {
  /**
    * Searches for a more recent version of the provided AMI.
    *
    * For Ubuntu AMIs it will check the Name to match the distribution, etc.
    * For Machine images and amigo AMIs it will check the tags.
    *
    * In all cases it should check the virtualisation and architecture as
    * well as using the date to see what is newer.
    */
  def amiUpgrade(ami: AMI, allAmis: Set[AMI]): Option[AMI] = {
    val candidateAmis = allAmis.view
      .filter(_.ownerId == ami.ownerId)
      .filter(newerThan(ami))
      .filter(_.region == ami.region)
      .filter(_.virtualizationType == ami.virtualizationType)
      .filter(_.architecture == ami.architecture)
      .filter(_.rootDeviceType == ami.rootDeviceType)
      .filter(_.hypervisor == ami.hypervisor)
      .filter(_.creationDate.nonEmpty)

    val upgrades = {
      // filter on additional ami-creator specific properties where appropriate
      if (isUbuntu(ami)) candidateAmis.filter(isUbuntuUpgrade(ami))
      else if (isMachineImages(ami)) candidateAmis.filter(isMachineImagesUpgrade(ami))
      else if (isAmigo(ami)) candidateAmis.filter(isAmigoUpgrade(ami))
      // with Amazon Linux, it is enough to have matched the above conditions
      else if (isAmazon(ami)) candidateAmis
      // unknown ami type, cannot recommend upgrades
      else Set.empty
    }
    upgrades
      .toList
      .sortBy(_.creationDate.get.getMillis)
      .lastOption
  }

  def amiWithUpgrade(allAmis: Set[AMI])(ami: AMI): AMI = {
    ami.copy(upgrade = amiUpgrade(ami, allAmis))
  }

  /**
    * Ubuntu upgrade is newer image that shares the same name (apart from date suffix)
    */
  private val UbuntuIdentifier = "(.*)-[\\d\\.]+".r
  private[prism] def isUbuntuUpgrade(ubuntuAmi: AMI)(candidateAmi: AMI): Boolean = {
    (for {
      amiName <- ubuntuAmi.name
      candidateName <- candidateAmi.name
    } yield {
      (amiName, candidateName) match {
        case (UbuntuIdentifier(amiIdentifier), UbuntuIdentifier(candidateIdentifier)) =>
          amiIdentifier == candidateIdentifier
        case _ => false
      }
    }) getOrElse false
  }

  def isObsoleteUbuntu(ubuntuAmi: AMI): Boolean = {
    val DistExtractor = ".*/ubuntu-(\\w+?)-(\\d{2}.\\d{2})-.*".r
    ubuntuAmi.name.fold(false) {
      case DistExtractor(distName, distNumber) =>
        List("15.04", "14.10", "13.10", "13.04", "12.10", "11.10", "11.04", "10.10", "10.04").contains(distNumber)
      case _ =>
        false
    }
  }

  private[prism] def isMachineImagesUpgrade(machineImagesAmi: AMI)(candidateAmi: AMI): Boolean = {
    (for {
      amiImageName <- machineImagesAmi.tags.get("ImageName")
      candidateImageName <- candidateAmi.tags.get("ImageName")
      amiBranch <- machineImagesAmi.tags.get("Branch")
      candidateBranch <- candidateAmi.tags.get("Branch")
    } yield {
      amiImageName == candidateImageName && amiBranch == candidateBranch
    }) getOrElse false
  }

  private[prism] def isAmigoUpgrade(amigoAmi: AMI)(candidateAmi: AMI): Boolean = {
    (for {
      amiStage <- amigoAmi.tags.get("AmigoStage")
      candidateStage <- candidateAmi.tags.get("AmigoStage")
      amiRecipe <- amigoAmi.tags.get("Recipe")
      candidateRecipe <- candidateAmi.tags.get("Recipe")
    } yield {
      amiStage == candidateStage && amiRecipe == candidateRecipe
    }) getOrElse false
  }

  def isUbuntu(ami: AMI): Boolean = ami.ownerId == "099720109477"
  def isAmazon(ami: AMI): Boolean = ami.ownerId == "137112412989"
  def isMachineImages(ami: AMI): Boolean = ami.tags.get("BuildName").exists(_.endsWith("-machine-images"))
  def isAmigo(ami: AMI): Boolean = ami.tags.get("BuiltBy").contains("amigo")
  def isUnknown(ami: AMI): Boolean = {
    !(isUbuntu(ami) || isAmazon(ami) || isAmigo(ami) || isMachineImages(ami))
  }

  def owner(ami: AMI): String = {
    if (isUbuntu(ami)) "Ubuntu"
    else if (isAmazon(ami)) "Amazon"
    else if (isAmigo(ami)) "Amigo"
    else if (isMachineImages(ami)) "Machine-images"
    else ami.ownerId
  }

  private[prism] def newerThan(sourceAmi: AMI)(candidateAmi: AMI): Boolean = {
    (for {
      candidateDate <- candidateAmi.creationDate
      amiDate <- sourceAmi.creationDate
    } yield {
      candidateDate.isAfter(amiDate)
    }) getOrElse false
  }
}
