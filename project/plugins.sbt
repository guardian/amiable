// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.6")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.10.4")
libraryDependencies += "org.vafer" % "jdeb" % "1.12" artifacts (Artifact(
  "jdeb",
  "jar",
  "jar"
))

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// web plugins
addSbtPlugin("com.github.sbt" % "sbt-coffeescript" % "2.0.1")

addSbtPlugin("com.github.sbt" % "sbt-less" % "2.0.1")

addSbtPlugin("com.github.sbt" % "sbt-jshint" % "2.0.1")

addSbtPlugin("com.github.sbt" % "sbt-rjs" % "2.0.0")

addSbtPlugin("com.github.sbt" % "sbt-digest" % "2.0.0")

addSbtPlugin("io.github.irundaia" % "sbt-sassify" % "1.5.2")
