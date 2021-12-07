// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.20")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.1")
addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "1.1.18")
libraryDependencies += "org.vafer" % "jdeb" % "1.3" artifacts (Artifact("jdeb", "jar", "jar"))

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.7")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")

addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.11")
