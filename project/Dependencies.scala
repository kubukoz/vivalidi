import sbt._

object Dependencies {
  val kindProjector  = "org.spire-math"       %% "kind-projector"   % Versions.kindProjector
  val catsCore       = "org.typelevel"        %% "cats-core"        % Versions.cats
  val catsEffect     = "org.typelevel"        %% "cats-effect"      % Versions.catsEffect
  val catsEffectLaws = "org.typelevel"        %% "cats-effect-laws" % Versions.catsEffect
  val shapeless      = "com.chuusai"          %% "shapeless"        % Versions.shapeless
  val scalatest      = "org.scalatest"        %% "scalatest"        % Versions.scalatest % Test
  val simulacrum     = "com.github.mpilquist" %% "simulacrum"       % Versions.simulacrum
  val scalacheckCore = "org.scalacheck"       %% "scalacheck"       % Versions.scalacheck % Test

  val cats       = Seq(catsCore, catsEffect % Test, catsEffectLaws % Test)
  val scalacheck = Seq(scalacheckCore)
}

object Versions {
  val catsEffect    = "2.0.0"
  val cats          = "2.0.0"
  val kindProjector = "0.10.3"
  val scalatest     = "3.1.0"
  val shapeless     = "2.3.3"
  val simulacrum    = "0.13.0"
  val scalacheck    = "1.14.2"
}
