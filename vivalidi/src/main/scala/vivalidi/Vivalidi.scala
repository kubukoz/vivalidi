package vivalidi

import cats.temp.par._
import cats.{Applicative, ApplicativeError, Parallel}
import shapeless.HNil
import vivalidi.builder.VivalidiBuilder

import scala.language.higherKinds

object Vivalidi {

  def apply[Subject, F[_]](implicit F: Par[F]): PartiallyApplied[Subject, F, F.ParAux] = {
    new PartiallyApplied[Subject, F, F.ParAux]()(F.parallel)
  }

  sealed class PartiallyApplied[Subject, F[_], P[_]](implicit F: Parallel[F, P]) {

    def init[E](implicit E: ApplicativeError[F, E]): VivalidiBuilder[Subject, E, HNil, F, P] = {
      val initialMemory: F[HNil] = Applicative[F].pure(HNil)

      new VivalidiBuilder[Subject, E, HNil, F, P](_ => initialMemory)
    }
  }

}
