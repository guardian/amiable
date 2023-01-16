// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.19")

// sbt-native-packager cannot be updated to >1.9.9 until Play supports scala-xml 2
addSbtPlugin(
  "com.github.sbt" % "sbt-native-packager" % "1.9.9"
) // scala-steward:off
addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "1.1.18")
libraryDependencies += "org.vafer" % "jdeb" % "1.10" artifacts (Artifact(
  "jdeb",
  "jar",
  "jar"
))

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")

/*
 * This is required for Scala Steward to run until SBT plugins all migrated to scala-xml 2.
 * See https://github.com/scala-steward-org/scala-steward/blob/13d63e8ae98a714efcdac2c7af18f004130512fa/project/plugins.sbt#L16-L19
 */
libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.10")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")

addSbtPlugin("io.github.irundaia" % "sbt-sassify" % "1.5.2")
