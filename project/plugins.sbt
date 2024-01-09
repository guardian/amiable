// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.1")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
libraryDependencies += "org.vafer" % "jdeb" % "1.10" artifacts (Artifact(
  "jdeb",
  "jar",
  "jar"
))

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// web plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.10")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")

addSbtPlugin("io.github.irundaia" % "sbt-sassify" % "1.5.2")
