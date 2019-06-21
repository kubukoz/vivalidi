import sbt._

object Dependencies {
  val kindProjector  = "org.spire-math"       %% "kind-projector"   % Versions.kindProjector
  val catsCore       = "org.typelevel"        %% "cats-core"        % Versions.cats
  val catsParTemp    = "io.chrisdavenport"    %% "cats-par"         % Versions.catsParTemp
  val catsEffect     = "org.typelevel"        %% "cats-effect"      % Versions.catsEffect
  val catsEffectLaws = "org.typelevel"        %% "cats-effect-laws" % Versions.catsEffect
  val shapeless      = "com.chuusai"          %% "shapeless"        % Versions.shapeless
  val scalatest      = "org.scalatest"        %% "scalatest"        % Versions.scalatest % Test
  val simulacrum     = "com.github.mpilquist" %% "simulacrum"       % Versions.simulacrum
  val scalacheckCore = "org.scalacheck"       %% "scalacheck"       % Versions.scalacheck % Test

  val cats       = Seq(catsCore, catsParTemp, catsEffect % Test, catsEffectLaws % Test)
  val scalacheck = Seq(scalacheckCore)

  val scalaz = Seq(
    "org.scalaz" %% "scalaz-zio" % Versions.zio % Test
  )
}

object Versions {
  val zio           = "0.2.11"
  val catsEffect    = "1.2.0"
  val cats          = "1.6.1"
  val catsParTemp   = "0.2.1"
  val kindProjector = "0.9.9"
  val scalatest     = "3.0.8"
  val shapeless     = "2.3.3"
  val simulacrum    = "0.13.0"
  val scalacheck    = "1.14.0"
}
