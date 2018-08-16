package vivalidi

import cats.{Applicative, Monad, Parallel}
import cats.temp.par._
import shapeless.HNil
import vivalidi.builder.VivalidiBuilder

import scala.language.higherKinds

object Vivalidi {

  def init[Subject, Err[_]: Applicative, F[_]](implicit F: Par[F]): VivalidiBuilder[Subject, Err, HNil, F, F.ParAux] = {
    val ErrAp: Applicative[Err] = Applicative[Err]

    implicit val M: Monad[F]              = Par[F].parallel.monad
    implicit val P: Parallel[F, F.ParAux] = F.parallel

    val initialMemory: F[Err[HNil]] = Applicative[F].compose[Err].pure(HNil)

    new VivalidiBuilder[Subject, Err, HNil, F, F.ParAux](_ => initialMemory)(M, ErrAp, P)
  }
}
