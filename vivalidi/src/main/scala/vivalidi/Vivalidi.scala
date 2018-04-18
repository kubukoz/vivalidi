package vivalidi

import cats.Applicative
import shapeless.HNil
import vivalidi.builder.VivalidiBuilder

object Vivalidi {

  def init[Subject, Errors[_]: Applicative, F[_]: Applicative]: VivalidiBuilder[Subject, Errors, HNil, F] =
    new VivalidiBuilder[Subject, Errors, HNil, F](_ => Applicative[F].compose[Errors].pure(HNil))
}
