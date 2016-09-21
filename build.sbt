name := """amiable"""

version := "1.0-SNAPSHOT"

enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.typesafe.akka" %% "akka-agent" % "2.4.2",
  "io.reactivex" %% "rxscala" % "0.26.0",
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.10.67",
  "com.gu" %% "play-googleauth" % "0.5.0",
  specs2 % Test,
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  "org.mockito" % "mockito-core" % "1.10.19" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

javaOptions in Test += "-Dconfig.file=conf/application.test.conf"

PlayKeys.playDefaultPort := 9101

// Allow building both ways on Teamcity
addCommandAlias("riffRaffArtifact", "riffRaffUpload")

packageName in Universal := name.value
maintainer := "Guardian Developers <dig.dev.software@theguardian.com>"
packageSummary := "AMIable"
packageDescription := """Web app for monitoring the use of AMIs"""
debianPackageDependencies := Seq("openjdk-8-jre-headless")

javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null",
  s"-Dconfig.file=/etc/${name.value}.conf",
  "-J-XX:MaxRAMFraction=2",
  "-J-XX:InitialRAMFraction=2",
  "-J-XX:MaxMetaspaceSize=300m",
  "-J-XX:+PrintGCDetails",
  "-J-XX:+PrintGCDateStamps",
  s"-J-Xloggc:/var/log/${packageName.value}/gc.log"
)

import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd
serverLoading in Debian := Systemd
riffRaffPackageType := (packageBin in Debian).value

def env(key: String): Option[String] = Option(System.getenv(key))

riffRaffBuildIdentifier := env("BUILD_NUMBER").getOrElse("DEV")
riffRaffManifestBranch := env("BRANCH_NAME").getOrElse("unknown_branch")
riffRaffManifestVcsUrl := "git@github.com:guardian/amiable.git"
riffRaffUploadArtifactBucket := Some("riffraff-artifact")
riffRaffUploadManifestBucket := Some("riffraff-builds")
riffRaffArtifactResources  := Seq(
  riffRaffPackageType.value -> s"packages/${name.value}/${name.value}.deb",
  baseDirectory.value / "deploy.json" -> "deploy.json"
)
