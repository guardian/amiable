import com.typesafe.sbt.packager.debian.DebianPlugin.autoImport.Debian

name := """amiable"""

version := "1.2-SNAPSHOT"

enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging, SystemdPlugin)

scalaVersion := "2.13.7"

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

javaOptions in Test += "-Dconfig.file=conf/application.test.conf"

routesGenerator := InjectedRoutesGenerator

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-Xcheckinit",
  "-feature",
  "-nowarn",
  "-Ywarn-unused"
)

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  "com.typesafe.akka" %% "akka-agent" % "2.5.32",
  "io.reactivex" %% "rxscala" % "0.27.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.1",
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.12.134",
  "com.amazonaws" % "aws-java-sdk-ses" % "1.12.134",
  "com.google.code.gson" % "gson" % "2.8.9",
  "com.gu.play-googleauth" % "play-v28_2.13" % "2.2.2",
  "org.quartz-scheduler" % "quartz" % "2.3.2",
  "com.typesafe.play" %% "play-json-joda" % "2.10.0-RC5",
  specs2 % Test,
  "org.scalatest" %% "scalatest" % "3.2.10" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % "test",
  "org.mockito" % "mockito-core" % "4.2.0" % Test
)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

PlayKeys.playDefaultPort := 9101

packageName in Universal := name.value
maintainer := "Guardian Developers <dig.dev.software@theguardian.com>"
packageSummary := "AMIable"
packageDescription := """Web app for monitoring the use of AMIs"""
debianPackageDependencies := Seq("openjdk-8-jre-headless")

riffRaffPackageType := (packageBin in Debian).value
riffRaffArtifactResources  := Seq(
  riffRaffPackageType.value -> s"${name.value}/${name.value}.deb",
  baseDirectory.value / "riff-raff.yaml" -> "riff-raff.yaml",
  baseDirectory.value / "cdk/cdk.out/Amiable.template.json" -> "cloudformation/Amiable.template.json"
)
