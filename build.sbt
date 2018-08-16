import Dependencies._
import sbt.addCompilerPlugin
import sbtcrossproject.{crossProject, CrossType}

scalacOptions in ThisBuild ++= Options.flags
scalacOptions in (Compile, console) --= Options.consoleExclusions

val kindProjector = addCompilerPlugin(
  "org.spire-math" %% "kind-projector" % Versions.kindProjector
)

crossScalaVersions := Seq("2.12.4")

val vivalidiDeps = Seq(
  shapeless,
  scalatest
) ++ scalacheck ++ cats

val commonSettings = Seq(
  organization := "com.kubukoz",
  scalaVersion := "2.12.4",
  description := "Elegant, effect-agnostic validations for Scala DTOs",
  version := "0.1.0",
  kindProjector,
  libraryDependencies ++= vivalidiDeps
)

val publishSettings = Seq(
  organization := "com.kubukoz",
  homepage := Some(url("http://vivalidi.kubukoz.com")),
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(
    ScmInfo(url("https://github.com/kubukoz/vivalidi"), "scm:git:git@github.com:kubukoz/vivalidi.git")
  ),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  pomExtra := <developers>
    <developer>
      <id>kubukoz</id>
      <name>Jakub Kozłowski</name>
      <url>https://github.com/kubukoz</url>
    </developer>
  </developers>
)

val noPublishSettings =
  Seq(skip in publish := true, publishArtifact := false)

def makeDep(project: Project) = project % "compile->compile;test->test"

val vivalidi = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(commonSettings, libraryDependencies ++= vivalidiDeps)
  .settings(publishSettings)

val vivalidiJVM = vivalidi.jvm
val vivalidiJS  = vivalidi.js

val root = (project in file("."))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(noPublishSettings)
  .dependsOn(vivalidiJVM, vivalidiJS)
  .aggregate(vivalidiJVM, vivalidiJS)
