addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.17.0")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.4")

addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "1.0.0")

/* Make sure selenium is before scalajs-envs:
 * It pulls in "closure-compiler-java-6" which in turn bundles some old
 * guava stuff which in turn makes selenium fail.
 */
libraryDependencies ~=
  ("org.seleniumhq.selenium" % "selenium-server" % "3.141.59" +: _)

Compile / unmanagedSourceDirectories ++= {
  val root = baseDirectory.value.getParentFile
  Seq(root / "seleniumJSEnv/src/main/scala")
}

Compile / sources += {
  val root = baseDirectory.value.getParentFile
  root / "seleniumJSEnv/src/test/scala/org/scalajs/jsenv/selenium/TestCapabilities.scala"
}
