package vivalidi

import cats.temp.par.Par
import cats.{Applicative, ApplicativeError}
import shapeless.HNil
import vivalidi.builder.VivalidiBuilder

import scala.language.higherKinds

object Vivalidi {

  def apply[Subject, F[_]: Par]: PartiallyApplied[Subject, F] = {
    new PartiallyApplied[Subject, F]
  }

  sealed class PartiallyApplied[Subject, F[_]] {

    def init[E](implicit F: Par[F], E: ApplicativeError[F, E]): VivalidiBuilder[Subject, E, HNil, F] = {
      val initialMemory: F[HNil] = Applicative[F].pure(HNil)

      new VivalidiBuilder[Subject, E, HNil, F](_ => initialMemory)
    }
  }

}
