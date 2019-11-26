package vivalidi

import cats.data.Kleisli
import cats.ApplicativeError
import shapeless.HNil
import vivalidi.builder.VivalidiBuilder

import scala.language.higherKinds
import cats.Parallel

object Vivalidi {

  def apply[Subject, F[_]]: PartiallyApplied[Subject, F] = {
    new PartiallyApplied[Subject, F]
  }

  final class PartiallyApplied[Subject, F[_]] private[Vivalidi] {

    def init[E](implicit F: Parallel[F], E: ApplicativeError[F, E]): VivalidiBuilder[Subject, E, HNil, F] = {
      new VivalidiBuilder[Subject, E, HNil, F](Kleisli.pure(HNil))
    }
  }

}
