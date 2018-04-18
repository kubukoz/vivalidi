package vivalidi.syntax

import cats.syntax.functor._
import cats.syntax.traverse._
import cats.{Applicative, Traverse}

trait SelectorSyntax {
  def withWhole[Subject, Field](f: Subject => Field): Subject => (Field, Subject) = s => (f(s), s)
  def pass[Subject]: Subject => Subject                                           = identity
}

class SyncValidatorLifter[A, B, Errors[_]](val validator: A => Errors[B]) extends AnyVal {
  def liftF[F[_]: Applicative]: A => F[Errors[B]] = validator.andThen(Applicative[F].pure)
}

trait ValidatorSyntax {

  //async variant
  def sequencing[F[_]: Applicative, E[_]: Applicative, G[_]: Traverse, Field, Output](
    f: Field => F[E[Output]]): G[Field] => F[E[G[Output]]] = _.traverse(f).map(_.sequence)

  //sync variant
  def sequencing[E[_]: Applicative, G[_]: Traverse, Field, Output](f: Field => E[Output]): G[Field] => E[G[Output]] =
    _.traverse(f)

  implicit def syncValidatorLifter[A, B, Errors[_]](validator: A => Errors[B]): SyncValidatorLifter[A, B, Errors] =
    new SyncValidatorLifter(validator)
}
