import Dependencies._
import sbt.addCompilerPlugin
import sbtcrossproject.{crossProject, CrossType}

inThisBuild(List(
  organization := "com.kubukoz",
  homepage := Some(url("https://github.com/kubukoz/vivalidi")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "kubukoz",
      "Jakub Kozłowski",
      "kubukoz@gmail.com",
      url("https://kubukoz.com")
    )
  )
))

scalacOptions in ThisBuild ++= Options.flags
scalacOptions in (Compile, console) --= Options.consoleExclusions

val kindProjector = addCompilerPlugin(
  "org.spire-math" %% "kind-projector" % Versions.kindProjector
)

crossScalaVersions in ThisBuild := Seq("2.12.7")
resolvers in ThisBuild += Resolver.sonatypeRepo("snapshots")

val vivalidiDeps = Seq(
  shapeless,
  scalatest
) ++ scalacheck ++ cats ++ scalaz

val commonSettings = Seq(
  organization := "com.kubukoz",
  scalaVersion := "2.12.7",
  description := "Elegant, effect-agnostic validations for Scala DTOs",
  kindProjector,
  libraryDependencies ++= vivalidiDeps
)

val noPublishSettings =
  Seq(skip in publish := true)

def makeDep(project: Project) = project % "compile->compile;test->test"

val vivalidi = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(commonSettings, libraryDependencies ++= vivalidiDeps)

val vivalidiJVM = vivalidi.jvm.settings(
  resolvers += Resolver.sonatypeRepo("releases"),
  mimaPreviousArtifacts := Set("com.kubukoz" %%% "vivalidi" % "0.3.0")
)

val vivalidiJS  = vivalidi.js

val root = (project in file("."))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .dependsOn(vivalidiJVM, vivalidiJS)
  .aggregate(vivalidiJVM, vivalidiJS)
