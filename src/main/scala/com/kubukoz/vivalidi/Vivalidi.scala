package com.kubukoz.vivalidi

import cats.data.NonEmptyList
import cats.instances.function._
import cats.kernel.Semigroup
import cats.syntax.all._
import cats.{Applicative, Apply, Traverse}
import shapeless.ops.hlist.Reverse
import shapeless.{::, Generic, HList, HNil}

object Vivalidi {

  def init[Subject, Errors[_]: Applicative, F[_]: Applicative]: VivalidiBuilder[Subject, Errors, HNil, F] =
    new VivalidiBuilder[Subject, Errors, HNil, F](_ => Applicative[F].compose[Errors].pure(HNil))

  implicit class SyncValidatorToAsync[Errors[_], A, B](val validator: A => Errors[B]) extends AnyVal {
    def liftF[G[_]: Applicative]: A => G[Errors[B]] = validator.andThen(Applicative[G].pure)
  }

  object dsl {
    //selectors
    def withWhole[Subject, Field](f: Subject => Field): Subject => (Field, Subject) = s => (f(s), s)
    def pass[Subject]: Subject => Subject                                           = identity

    //checkers
    def sequencing[F[_]: Applicative, E[_]: Applicative, G[_]: Traverse, Field, Output](
      f: Field => F[E[Output]]): G[Field] => F[E[G[Output]]] = _.traverse(f).map(_.sequence)
  }
}

final class VivalidiBuilder[Subject, Errors[_], SuccessRepr <: HList, F[_]](memory: Subject => F[Errors[SuccessRepr]])(
  implicit F: Applicative[F],
  E: Applicative[Errors]) {

  type SyncValidator[I, O]  = I => Errors[O]
  type AsyncValidator[I, O] = I => F[Errors[O]]

  import Vivalidi._

  def pure[Field](value: Field): VivalidiBuilder[Subject, Errors, Field :: SuccessRepr, F] =
    just(Function.const(value))

  def just[Field](toField: Subject => Field): VivalidiBuilder[Subject, Errors, Field :: SuccessRepr, F] =
    sync(toField)(E.pure)

  def sync[Field, Output](toField: Subject => Field)(
    checkFirst: SyncValidator[Field, Output],
    checkMore: SyncValidator[Field, Output]*): VivalidiBuilder[Subject, Errors, Output :: SuccessRepr, F] = {

    async(toField)(checkFirst.liftF[F], checkMore.map(_.liftF[F]): _*)
  }

  def async[Field, Output, Actual](toField: Subject => Field)(
    checkFirst: AsyncValidator[Field, Output],
    checkMore: AsyncValidator[Field, Output]*): VivalidiBuilder[Subject, Errors, Output :: SuccessRepr, F] = {

    //configurable?
    val outputSemigroup = Semigroup.instance[Output]((a, _) => a)

    val checkAll: AsyncValidator[Field, Output] =
      NonEmptyList(checkFirst, checkMore.toList).reduce(validatorSemigroup[Field, Output](outputSemigroup))

    val mapAndCheck = toField.andThen(checkAll)

    new VivalidiBuilder[Subject, Errors, Output :: SuccessRepr, F](
      applicativeStack.map2(mapAndCheck, memory)(_ :: _)
    )
  }

  def to[Success]: To[Success] = new To[Success]

  class To[Success] {

    def run[SuccessReverseRepr <: HList](subject: Subject)(
      implicit gen: Generic.Aux[Success, SuccessReverseRepr],
      rev: Reverse.Aux[SuccessRepr, SuccessReverseRepr]): F[Errors[Success]] = {
      applicativeStack.map(memory)(f => gen.from(rev(f)))(subject)
    }
  }

  private def applicativeStackT[T] = Applicative[T => ?].compose[F].compose[Errors]
  private val applicativeStack     = applicativeStackT[Subject]
  private def validatorSemigroup[I, T: Semigroup]: Semigroup[AsyncValidator[I, T]] = {
    Apply.semigroup[AsyncValidator[I, ?], T](applicativeStackT[I], Semigroup[T])
  }
}
