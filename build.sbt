import com.gu.riffraff.artifact.RiffRaffArtifact
import com.gu.riffraff.artifact.RiffRaffArtifact.autoImport._


name := """amiable"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, RiffRaffArtifact)
  .settings(
    packageName in Universal := normalizedName.value,
    topLevelDirectory in Universal := Some(normalizedName.value),
    riffRaffPackageType := (packageZipTarball in Universal).value
  )

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  "org.mockito" % "mockito-core" % "1.10.19" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
