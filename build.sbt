import sbt.Keys._

import org.scalajs.sbtplugin.ScalaJSCrossVersion

import org.openqa.selenium.Capabilities

import org.scalajs.jsenv.selenium.SeleniumJSEnv
import org.scalajs.jsenv.selenium.TestDrivers

val commonSettings: Seq[Setting[_]] = Seq(
  version := "2.0.0-SNAPSHOT",
  organization := "org.scala-js",
  scalaVersion := crossScalaVersions.value.head,
  crossScalaVersions := Seq("2.12.20", "2.13.16"),
  scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings"),

  homepage := Some(url("http://scala-js.org/")),
  licenses += ("BSD New",
      url("https://github.com/scala-js/scala-js-env-selenium/blob/main/LICENSE")),
  scmInfo := Some(ScmInfo(
      url("https://github.com/scala-js/scala-js-env-selenium"),
      "scm:git:git@github.com:scala-js/scala-js-env-selenium.git",
      Some("scm:git:git@github.com:scala-js/scala-js-env-selenium.git"))),
  testOptions += Tests.Argument(TestFramework("com.novocode.junit.JUnitFramework"), "-v", "-a")
)

val mimaSettings = Seq(
  mimaBinaryIssueFilters ++= BinaryIncompatibilities.SeleniumJSEnv,
  mimaFailOnNoPrevious := false,
  mimaPreviousArtifacts := {
    // Released versions - will be Set("2.0.0", "2.0.1", etc.) after releases
    val all: Set[String] = Set.empty

    // Exclusion predicates per Scala binary version
    // Example: Map("3" -> (_.startsWith("2.0."))) would exclude 2.0.x versions for Scala 3
    val exclusions: Map[String, String => Boolean] = Map.empty

    all
      .filterNot(exclusions.getOrElse(scalaBinaryVersion.value, _ => false))
      .map(v => organization.value %% name.value % v)
  }
)

val testSettings: Seq[Setting[_]] = commonSettings ++ Seq(
  jsEnv := new SeleniumJSEnv(TestDrivers.fromEnv),
  scalaJSUseMainModuleInitializer := true
)

// We'll need the name scalajs-env-selenium for the `seleniumJSEnv` project
name := "root"

lazy val seleniumJSEnv: Project = project.
  settings(commonSettings).
  settings(mimaSettings).
  settings(
    name := "scalajs-env-selenium",

    libraryDependencies ++= Seq(
        /* Make sure selenium is before scalajs-envs-test-kit:
         * It pulls in "closure-compiler-java-6" which in turn bundles some old
         * guava stuff which in turn makes selenium fail.
         */
        "org.seleniumhq.selenium" % "selenium-java" % "4.35.0",
        "org.scala-js" %% "scalajs-js-envs" % "1.4.0",
        "com.google.jimfs" % "jimfs" % "1.3.1",
        "org.scala-js" %% "scalajs-js-envs-test-kit" % "1.4.0" % Test,
        "com.novocode" % "junit-interface" % "0.11" % Test
    ),

    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomExtra := (
      <developers>
        <developer>
          <id>nicolasstucki</id>
          <name>Nicolas Stucki</name>
          <url>https://github.com/nicolasstucki/</url>
        </developer>
        <developer>
          <id>sjrd</id>
          <name>Sébastien Doeraene</name>
          <url>https://github.com/sjrd/</url>
        </developer>
        <developer>
          <id>gzm0</id>
          <name>Tobias Schlatter</name>
          <url>https://github.com/gzm0/</url>
        </developer>
      </developers>
    ),
    pomIncludeRepository := { _ => false },

    // The chrome driver seems to not deal with parallelism very well (#47).
    (Test / parallelExecution) := false
  )

lazy val seleniumJSEnvTest: Project = project.
  enablePlugins(ScalaJSPlugin).
  enablePlugins(ScalaJSJUnitPlugin).
  settings(testSettings)

lazy val seleniumJSHttpEnvTest: Project = project.
  enablePlugins(ScalaJSPlugin).
  enablePlugins(ScalaJSJUnitPlugin).
  settings(testSettings).
  settings(
    jsEnv := {
      new SeleniumJSEnv(
          TestDrivers.fromEnv,
          SeleniumJSEnv.Config()
            .withMaterializeInServer("tmp", "http://localhost:8080/tmp/")
      )
    },
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }
  )