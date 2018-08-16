package vivalidi.builder

import cats.data.NonEmptyList
import cats.implicits._
import cats.kernel.Semigroup
import cats.temp.par.Par
import cats.{ApplicativeError, Functor}
import shapeless.ops.hlist.Reverse
import shapeless.{::, Generic, HList}

import scala.language.higherKinds

private[vivalidi] final class VivalidiBuilder[Subject, Err, SuccessRepr <: HList, F[_]](
  memory: Subject => F[SuccessRepr])(implicit F: ApplicativeError[F, Err], P: Par[F]) {

  type PureValidator[I, O]             = I => Either[Err, O]
  type AsyncValidator[I, O]            = I => F[O]
  private type AsyncValidatorPar[I, O] = I => P.ParAux[O]

  def pure[Field](value: Field): VivalidiBuilder[Subject, Err, Field :: SuccessRepr, F] =
    just(Function.const(value))

  def just[Field](toField: Subject => Field): VivalidiBuilder[Subject, Err, Field :: SuccessRepr, F] =
    sync(toField)(_.asRight)

  def sync[Field, Output](toField: Subject => Field)(
    checkFirst: PureValidator[Field, Output],
    checkMore: PureValidator[Field, Output]*): VivalidiBuilder[Subject, Err, Output :: SuccessRepr, F] = {

    async(toField)(checkFirst(_).liftTo[F],
                   checkMore.map(validator => (input: Field) => validator(input).liftTo[F]): _*)
  }

  def async[Field, Output, Actual](toField: Subject => Field)(
    checkFirst: AsyncValidator[Field, Output],
    checkMore: AsyncValidator[Field, Output]*): VivalidiBuilder[Subject, Err, Output :: SuccessRepr, F] = {

    //semigroup for success values in case of multiple validations on a single field
    //configurable?
    val outputSemigroup = Semigroup.instance[Output]((a, _) => a)

    val checkAll: AsyncValidator[Field, Output] =
      NonEmptyList(checkFirst, checkMore.toList).reduce(validatorSemigroup[Field, Output](outputSemigroup))

    val mapAndCheck: AsyncValidator[Subject, Output] = toField.andThen(checkAll)

    new VivalidiBuilder[Subject, Err, Output :: SuccessRepr, F](
      //memory first to maintain order of errors in case of failure
      parMapV(memory, mapAndCheck)((memory, field) => field :: memory)
    )
  }

  def to[Success]: To[Success] = new To[Success]

  class To[Success] private[vivalidi] {

    def run[SuccessReverseRepr <: HList](implicit gen: Generic.Aux[Success, SuccessReverseRepr],
                                         rev: Reverse.Aux[SuccessRepr, SuccessReverseRepr]): Subject => F[Success] = {
      functorStack[Subject].map(memory)(f => gen.from(rev(f)))
    }
  }

  private def functorStack[T]: Functor[λ[A => T => F[A]]] =
    Functor[T => ?].compose[F]

  private def validatorSemigroup[I, T: Semigroup]: Semigroup[AsyncValidator[I, T]] = parMapV(_, _)(Semigroup[T].combine)

  //runs validating functions in parallel, combines results with `f`
  private def parMapV[I, O1, O2, O3](v1: AsyncValidator[I, O1], v2: AsyncValidator[I, O2])(
    f: (O1, O2) => O3): AsyncValidator[I, O3] = { i =>
    (v1(i), v2(i)).parMapN((aE, bE) => f(aE, bE))(P.parallel)
  }
}
