package vivalidi

import cats.temp.par._
import cats.{Applicative, ApplicativeError}
import shapeless.HNil
import vivalidi.builder.VivalidiBuilder

import scala.language.higherKinds

object Vivalidi {

  def init[Subject, F[_], E](implicit F: Par[F],
                             E: ApplicativeError[F, E]): VivalidiBuilder[Subject, E, HNil, F, F.ParAux] = {
    val initialMemory: F[HNil] = Applicative[F].pure(HNil)

    new VivalidiBuilder[Subject, E, HNil, F, F.ParAux](_ => initialMemory)(E, F.parallel)
  }
}
