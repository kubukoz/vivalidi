package vivalidi.builder

import cats.data.NonEmptyList
import cats.implicits._
import cats.kernel.Semigroup
import cats.{Applicative, Functor, Monad, Parallel}
import shapeless.ops.hlist.Reverse
import shapeless.{::, Generic, HList}
import vivalidi.syntax

import scala.language.higherKinds

private[vivalidi] final class VivalidiBuilder[Subject, Errors[_], SuccessRepr <: HList, F[_]: Monad, P[_]](
  memory: Subject => F[Errors[SuccessRepr]])(implicit E: Applicative[Errors], P: Parallel[F, P]) {

  type SyncValidator[I, O]             = I => Errors[O]
  type AsyncValidator[I, O]            = I => F[Errors[O]]
  private type AsyncValidatorPar[I, O] = I => P[Errors[O]]

  def pure[Field](value: Field): VivalidiBuilder[Subject, Errors, Field :: SuccessRepr, F, P] =
    just(Function.const(value))

  def just[Field](toField: Subject => Field): VivalidiBuilder[Subject, Errors, Field :: SuccessRepr, F, P] =
    sync(toField)(E.pure)

  def sync[Field, Output](toField: Subject => Field)(
    checkFirst: SyncValidator[Field, Output],
    checkMore: SyncValidator[Field, Output]*): VivalidiBuilder[Subject, Errors, Output :: SuccessRepr, F, P] = {
    import syntax.validators._

    async(toField)(checkFirst.liftF[F], checkMore.map(_.liftF[F]): _*)
  }

  def async[Field, Output, Actual](toField: Subject => Field)(
    checkFirst: AsyncValidator[Field, Output],
    checkMore: AsyncValidator[Field, Output]*): VivalidiBuilder[Subject, Errors, Output :: SuccessRepr, F, P] = {

    //configurable?
    val outputSemigroup = Semigroup.instance[Output]((a, _) => a)

    val checkAll: AsyncValidator[Field, Output] =
      NonEmptyList(checkFirst, checkMore.toList).reduce(validatorSemigroup[Field, Output](outputSemigroup))

    val mapAndCheck: AsyncValidator[Subject, Output] = toField.andThen(checkAll)

    new VivalidiBuilder[Subject, Errors, Output :: SuccessRepr, F, P](
      parMapV(mapAndCheck, memory)(_ :: _)
    )
  }

  def to[Success]: To[Success] = new To[Success]

  class To[Success] private[vivalidi] {

    def run[SuccessReverseRepr <: HList](subject: Subject)(
      implicit gen: Generic.Aux[Success, SuccessReverseRepr],
      rev: Reverse.Aux[SuccessRepr, SuccessReverseRepr]): F[Errors[Success]] = {
      functorStack[Subject].map(memory)(f => gen.from(rev(f)))(subject)
    }
  }

  private def functorStack[T]: Functor[λ[A => T => F[Errors[A]]]] =
    Functor[T => ?].compose[F].compose[Errors]

  private def validatorSemigroup[I, T: Semigroup]: Semigroup[AsyncValidator[I, T]] = parMapV(_, _)(Semigroup[T].combine)

  //runs validating functions in parallel, combines results with `f`
  private def parMapV[I, O1, O2, O3](v1: AsyncValidator[I, O1], v2: AsyncValidator[I, O2])(
    f: (O1, O2) => O3): AsyncValidator[I, O3] = { i =>
    (v1(i), v2(i)).parMapN((aE, bE) => (aE, bE).mapN(f))
  }
}
