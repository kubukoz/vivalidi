package vivalidi

import cats.Applicative
import cats.temp.par.Par
import shapeless.HNil
import vivalidi.builder.VivalidiBuilder

import scala.language.higherKinds

object Vivalidi {

  def init[Subject, Errors[_]: Applicative, F[_] : Par]: VivalidiBuilder[Subject, Errors, HNil, F] =
    new VivalidiBuilder[Subject, Errors, HNil, F](_ => Applicative[F](Par[F].parallel.monad).compose[Errors].pure(HNil))
}
