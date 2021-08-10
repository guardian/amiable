name := """amiable"""

version := "1.0-SNAPSHOT"

enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging, SystemdPlugin)

scalaVersion := "2.11.8"

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
  "-Yinline-warnings",
  "-Ywarn-unused"
)

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.typesafe.akka" %% "akka-agent" % "2.4.2",
  "io.reactivex" %% "rxscala" % "0.26.0",
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.11.557",
  "com.amazonaws" % "aws-java-sdk-ses" % "1.11.557",
  "com.gu" %% "play-googleauth" % "0.7.7",
  "org.quartz-scheduler" % "quartz" % "2.2.3",
  "com.typesafe.play" %% "play-json-joda" % "2.7.3",
  specs2 % Test,
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  "org.mockito" % "mockito-core" % "1.10.19" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

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
