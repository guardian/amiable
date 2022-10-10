import com.typesafe.sbt.packager.debian.DebianPlugin.autoImport.Debian

name := """amiable"""

version := "1.0-SNAPSHOT"

enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging, SystemdPlugin)

scalaVersion := "2.13.10"

Universal / javaOptions ++= Seq(
  "-Dpidfile.path=/dev/null",
  s"-Dconfig.file=/etc/${name.value}.conf",
  "-J-XX:MaxRAMFraction=2",
  "-J-XX:InitialRAMFraction=2",
  "-J-XX:MaxMetaspaceSize=300m",
  "-J-XX:+PrintGCDetails",
  "-J-XX:+PrintGCDateStamps",
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
val jacksonVersion         = "2.13.4"
val jacksonDatabindVersion = "2.13.4"

val jacksonOverrides = Seq(
  "com.fasterxml.jackson.core"     % "jackson-core",
  "com.fasterxml.jackson.core"     % "jackson-annotations",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
).map(_ % jacksonVersion)

val jacksonDatabindOverrides = Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion
)

val akkaSerializationJacksonOverrides = Seq(
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
  "com.fasterxml.jackson.module"     % "jackson-module-parameter-names",
  "com.fasterxml.jackson.module"     %% "jackson-module-scala",
).map(_ % jacksonVersion)

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  "com.typesafe.akka" %% "akka-agent" % "2.5.32",
  "io.reactivex" %% "rxscala" % "0.27.0",
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.12.319",
  "com.amazonaws" % "aws-java-sdk-ses" % "1.12.319",
  "com.google.code.gson" % "gson" % "2.9.1",
  "com.gu.play-googleauth" % "play-v28_2.13" % "2.2.6",
  "org.quartz-scheduler" % "quartz" % "2.3.2",
  "com.typesafe.play" %% "play-json-joda" % "2.10.0-RC6",
  specs2 % Test,
  "org.scalatest" %% "scalatest" % "3.2.14" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % "test",
  "org.mockito" % "mockito-core" % "4.8.0" % Test,
  "net.logstash.logback" % "logstash-logback-encoder" % "7.2"
) ++ jacksonDatabindOverrides ++ jacksonOverrides ++ akkaSerializationJacksonOverrides

/*
 * This is required for Scala Steward to run until SBT plugins all migrated to scala-xml 2.
 * See https://github.com/scala-steward-org/scala-steward/blob/13d63e8ae98a714efcdac2c7af18f004130512fa/project/plugins.sbt#L16-L19
 */
libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

PlayKeys.playDefaultPort := 9101

Universal / packageName := name.value
maintainer := "Guardian Developers <dig.dev.software@theguardian.com>"
packageSummary := "AMIable"
packageDescription := """Web app for monitoring the use of AMIs"""
debianPackageDependencies := Seq("openjdk-8-jre-headless")

riffRaffPackageType := (Debian / packageBin).value
riffRaffArtifactResources  := Seq(
  riffRaffPackageType.value -> s"${name.value}/${name.value}.deb",
  baseDirectory.value / "riff-raff.yaml" -> "riff-raff.yaml",
  baseDirectory.value / "cdk/cdk.out/Amiable-CODE.template.json" -> "cloudformation/Amiable-CODE.template.json",
  baseDirectory.value / "cdk/cdk.out/Amiable-PROD.template.json" -> "cloudformation/Amiable-PROD.template.json"
)
