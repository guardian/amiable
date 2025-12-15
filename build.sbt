import com.typesafe.sbt.packager.debian.DebianPlugin.autoImport.Debian

name := "amiable"

version := "1.0-SNAPSHOT"

enablePlugins(PlayScala, JDebPackaging, SystemdPlugin)

ThisBuild / scalaVersion := "3.3.7"

Universal / javaOptions ++= Seq(
  "-Dpidfile.path=/dev/null",
  s"-Dconfig.file=/etc/${name.value}.conf",
  "-J-XX:MaxRAMFraction=2",
  "-J-XX:InitialRAMFraction=2",
  "-J-XX:MaxMetaspaceSize=300m",
  "-J-Xlog:gc*",
  s"-J-Xloggc:/var/log/${packageName.value}/gc.log"
)

Test / javaOptions += "-Dconfig.file=conf/application.test.conf"

routesGenerator := InjectedRoutesGenerator

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-Xcheckinit",
  "-feature",
  "-nowarn",
  "-Ywarn-unused"
)

// https://github.com/orgs/playframework/discussions/11222
// Ensure all Amiable's Jackson dependencies use the same version for compatibility. Jackson will throw an error if the versions do not match.
val jacksonOverrides = {
  val jacksonVersion = "2.20.1"
  Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion,
    "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % jacksonVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,

    // The version numbering of jackson-annotations has diverged
    // See https://github.com/FasterXML/jackson-annotations/issues/307
    // and https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1#handling-of-jackson-annotations
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.20"
  )
}

val awsSdkVersion = "2.39.1"

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  "software.amazon.awssdk" % "cloudwatch" % awsSdkVersion,
  "software.amazon.awssdk" % "ses" % awsSdkVersion,
  "com.google.code.gson" % "gson" % "2.13.2",
  "com.gu.play-googleauth" %% "play-v30" % "30.1.0",
  "org.quartz-scheduler" % "quartz" % "2.5.2",
  "org.playframework" %% "play-json-joda" % "3.0.6",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test,
  "org.mockito" % "mockito-core" % "5.21.0" % Test,
  "net.logstash.logback" % "logstash-logback-encoder" % "7.3", // scala-steward:off
  // Transient dependency of Play. No newer version of Play with this vulnerability fixed.
  "ch.qos.logback" % "logback-classic" % "1.5.22"
) ++ jacksonOverrides

// See https://github.com/guardian/amiable/security/dependabot/35
excludeDependencies += ExclusionRule(
  organization = "net.sourceforge.htmlunit",
  name = "htmlunit"
)

PlayKeys.playDefaultPort := 9101

Universal / packageName := name.value
maintainer := "Guardian Developers <dig.dev.software@theguardian.com>"
packageSummary := "AMIable"
packageDescription := "Web app for monitoring the use of AMIs"
debianPackageDependencies := Seq("java-11-amazon-corretto-jdk:arm64")
