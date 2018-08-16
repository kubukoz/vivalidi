package vivalidi.syntax

import cats.syntax.traverse._
import cats.{Applicative, ApplicativeError, Traverse}

import scala.language.higherKinds

trait SelectorSyntax {
  def withWhole[Subject, Field](f: Subject => Field): Subject => (Field, Subject) = s => (f(s), s)
  def pass[Subject]: Subject => Subject                                           = identity
}

trait ValidatorSyntax {

  //async variant
  def sequencingF[F[_], E, G[_]: Traverse, Field, Output](f: Field => F[Output])(
    implicit F: ApplicativeError[F, E]): G[Field] => F[G[Output]] = _.traverse(f)

  //sync variant
  def sequencing[E[_]: Applicative, G[_]: Traverse, Field, Output](f: Field => E[Output]): G[Field] => E[G[Output]] =
    _.traverse(f)
}
