import Dependencies._
import sbt.addCompilerPlugin

scalacOptions in ThisBuild ++= Options.flags
scalacOptions in (Compile, console) --= Options.consoleExclusions

val macroParadise = addCompilerPlugin(("org.scalameta" % "paradise" % Versions.macroParadise).cross(CrossVersion.full))

val kindProjector = addCompilerPlugin(
  "org.spire-math" %% "kind-projector" % Versions.kindProjector
)

val vivalidiDeps = Seq(
  shapeless,
  scalatest
) ++ scalacheck ++ cats

val commonSettings = Seq(
  organization := "com.kubukoz",
  scalaVersion := "2.12.4",
  version := "0.0.1",
  macroParadise,
  kindProjector,
  libraryDependencies ++= vivalidiDeps
)

def makeDep(project: Project) = project % "compile->compile;test->test;it->it;it->test"

val vivalidi = (project in file(".")).settings(commonSettings, libraryDependencies ++= vivalidiDeps)
