import sbt.Keys._

import org.scalajs.sbtplugin.ScalaJSCrossVersion

import org.openqa.selenium.Capabilities

import org.scalajs.jsenv.selenium.SeleniumJSEnv
import org.scalajs.jsenv.selenium.TestCapabilities

val previousVersion: Option[String] = Some("1.1.0")

val newScalaBinaryVersionsInThisRelease: Set[String] =
  Set()

val commonSettings: Seq[Setting[_]] = Seq(
  version := "1.1.1-SNAPSHOT",
  organization := "org.scala-js",
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.11.12", "2.12.10", "2.13.1"),
  scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings"),

  homepage := Some(url("http://scala-js.org/")),
  licenses += ("BSD New",
      url("https://github.com/scala-js/scala-js-env-selenium/blob/master/LICENSE")),
  scmInfo := Some(ScmInfo(
      url("https://github.com/scala-js/scala-js-env-selenium"),
      "scm:git:git@github.com:scala-js/scala-js-env-selenium.git",
      Some("scm:git:git@github.com:scala-js/scala-js-env-selenium.git"))),
  testOptions += Tests.Argument(TestFramework("com.novocode.junit.JUnitFramework"), "-v", "-a")
)

val previousArtifactSetting = Def.settings(
  mimaPreviousArtifacts ++= {
    val scalaV = scalaVersion.value
    val scalaBinaryV = scalaBinaryVersion.value
    val thisProjectID = projectID.value
    previousVersion match {
      case None =>
        Set.empty
      case _ if newScalaBinaryVersionsInThisRelease.contains(scalaBinaryV) =>
        // New in this release, no binary compatibility to comply to
        Set.empty
      case Some(prevVersion) =>
        /* Filter out e:info.apiURL as it expects 0.6.7-SNAPSHOT, whereas the
         * artifact we're looking for has 0.6.6 (for example).
         */
        val prevExtraAttributes =
          thisProjectID.extraAttributes.filterKeys(_ != "e:info.apiURL")
        val prevProjectID =
          (thisProjectID.organization % thisProjectID.name % prevVersion)
            .cross(thisProjectID.crossVersion)
            .extra(prevExtraAttributes.toSeq: _*)
        Set(prevProjectID)
    }
  }
)

val jsEnvCapabilities = settingKey[org.openqa.selenium.Capabilities](
    "Capabilities of the SeleniumJSEnv")

val testSettings: Seq[Setting[_]] = commonSettings ++ Seq(
  jsEnvCapabilities := TestCapabilities.fromEnv,
  jsEnv := new SeleniumJSEnv(jsEnvCapabilities.value),
  scalaJSUseMainModuleInitializer := true
)

// We'll need the name scalajs-env-selenium for the `seleniumJSEnv` project
name := "root"

lazy val seleniumJSEnv: Project = project.
  settings(commonSettings).
  settings(
    name := "scalajs-env-selenium",

    libraryDependencies ++= Seq(
        /* Make sure selenium is before scalajs-envs-test-kit:
         * It pulls in "closure-compiler-java-6" which in turn bundles some old
         * guava stuff which in turn makes selenium fail.
         */
        "org.seleniumhq.selenium" % "selenium-server" % "3.141.59",
        "org.scala-js" %% "scalajs-js-envs" % "1.1.1",
        "com.google.jimfs" % "jimfs" % "1.1",
        "org.scala-js" %% "scalajs-js-envs-test-kit" % "1.1.1" % "test",
        "com.novocode" % "junit-interface" % "0.11" % "test"
    ),

    previousArtifactSetting,
    mimaBinaryIssueFilters ++= BinaryIncompatibilities.SeleniumJSEnv,

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
    parallelExecution in Test := false
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
          jsEnvCapabilities.value,
          SeleniumJSEnv.Config()
            .withMaterializeInServer("tmp", "http://localhost:8080/tmp/")
      )
    },
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }
  )
