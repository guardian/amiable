import com.typesafe.sbt.packager.debian.DebianPlugin.autoImport.Debian

name := "amiable"

version := "1.0-SNAPSHOT"

enablePlugins(PlayScala, JDebPackaging, SystemdPlugin)

ThisBuild / scalaVersion := "3.3.1"

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
// Ensure all Jackson versions used by Amiable are identical. Jackson will throw an error if the versions do not match.
val jacksonVersion = "2.17.2"
val jacksonOverrides = Seq(
  "com.fasterxml.jackson.core" % "jackson-core",
  "com.fasterxml.jackson.core" % "jackson-annotations",
  "com.fasterxml.jackson.core" % "jackson-databind",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
  "com.fasterxml.jackson.module" % "jackson-module-parameter-names",
  "com.fasterxml.jackson.module" %% "jackson-module-scala"
).map(_ % jacksonVersion)

val awsSdkVersion = "1.12.773"

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-ses" % awsSdkVersion,
  "com.google.code.gson" % "gson" % "2.10.1",
  "com.gu.play-googleauth" %% "play-v30" % "4.0.0",
  "org.quartz-scheduler" % "quartz" % "2.3.2",
  "org.playframework" %% "play-json-joda" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test,
  "org.mockito" % "mockito-core" % "5.8.0" % Test,
  "net.logstash.logback" % "logstash-logback-encoder" % "7.3", // scala-steward:off
  // Transient dependency of Play. No newer version of Play with this vulnerability fixed.
  "ch.qos.logback" % "logback-classic" % "1.4.14"
) ++ jacksonOverrides

PlayKeys.playDefaultPort := 9101

Universal / packageName := name.value
maintainer := "Guardian Developers <dig.dev.software@theguardian.com>"
packageSummary := "AMIable"
packageDescription := "Web app for monitoring the use of AMIs"
debianPackageDependencies := Seq("java-11-amazon-corretto-jdk:arm64")
