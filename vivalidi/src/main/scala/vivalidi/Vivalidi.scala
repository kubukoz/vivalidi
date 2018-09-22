package vivalidi

import cats.data.Kleisli
import cats.temp.par.Par
import cats.ApplicativeError
import shapeless.HNil
import vivalidi.builder.VivalidiBuilder

import scala.language.higherKinds

object Vivalidi {

  def apply[Subject, F[_]: Par]: PartiallyApplied[Subject, F] = {
    new PartiallyApplied[Subject, F]
  }

  final class PartiallyApplied[Subject, F[_]] private[Vivalidi] {

    def init[E](implicit F: Par[F], E: ApplicativeError[F, E]): VivalidiBuilder[Subject, E, HNil, F] = {
      new VivalidiBuilder[Subject, E, HNil, F](Kleisli.pure(HNil))
    }
  }

}
