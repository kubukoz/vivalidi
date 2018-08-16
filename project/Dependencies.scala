import sbt._

object Dependencies {
  val kindProjector  = "org.spire-math"       %% "kind-projector" % Versions.kindProjector
  val catsCore       = "org.typelevel"        %% "cats-core"      % Versions.cats
  val catsParTemp    = "io.chrisdavenport"    %% "cats-par"       % Versions.catsParTemp
  val catsEffect     = "org.typelevel"        %% "cats-effect"    % Versions.catsEffect
  val shapeless      = "com.chuusai"          %% "shapeless"      % Versions.shapeless
  val scalatest      = "org.scalatest"        %% "scalatest"      % Versions.scalatest % Test
  val simulacrum     = "com.github.mpilquist" %% "simulacrum"     % Versions.simulacrum
  val scalacheckCore = "org.scalacheck"       %% "scalacheck"     % Versions.scalacheck % Test

  val cats       = Seq(catsCore, catsParTemp, catsEffect)
  val scalacheck = Seq(scalacheckCore)
}

object Versions {
  val catsEffect    = "1.0.0-RC2"
  val cats          = "1.0.1"
  val catsParTemp   = "0.0.3"
  val kindProjector = "0.9.6"
  val scalatest     = "3.0.5"
  val macroParadise = "3.0.0-M11"
  val shapeless     = "2.3.3"
  val simulacrum    = "0.12.0"
  val scalacheck    = "1.13.5"
}
