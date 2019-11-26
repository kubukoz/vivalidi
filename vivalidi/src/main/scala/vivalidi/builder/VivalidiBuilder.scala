package vivalidi.builder

import cats.data.{Kleisli, NonEmptyList}
import cats.implicits._
import cats.kernel.Semigroup
import cats.ApplicativeError
import shapeless.ops.hlist.Reverse
import shapeless.{::, Generic, HList}

import scala.language.higherKinds
import cats.Parallel

final class VivalidiBuilder[Subject, Err, SuccessRepr <: HList, F[_]] private[vivalidi] (
  memory: Kleisli[F, Subject, SuccessRepr]
)(implicit F: ApplicativeError[F, Err], P: Parallel[F]) {

  type Builder[Field] = VivalidiBuilder[Subject, Err, Field :: SuccessRepr, F]

  type PureValidator[I, O]   = I => Either[Err, O]
  type AsyncValidator[I, O]  = I => F[O]
  type AsyncValidatorK[I, O] = Kleisli[F, I, O]

  def pure[Field](value: Field): Builder[Field] =
    just(Function.const(value))

  def liftF[Field](get: F[Field]): Builder[Field] =
    async(_ => ())(_ => get)

  def just[Field](toField: Subject => Field): Builder[Field] =
    sync(toField)(_.asRight)

  def sync[Field, Output](
    toField: Subject => Field
  )(checkFirst: PureValidator[Field, Output], checkMore: PureValidator[Field, Output]*): Builder[Output] = {
    val liftOne: PureValidator[Field, Output] => AsyncValidator[Field, Output] = v => v(_).liftTo[F]

    async(toField)(liftOne(checkFirst), checkMore.map(liftOne): _*)
  }

  def async[Field, Output](
    toField: Subject => Field
  )(checkFirst: AsyncValidator[Field, Output], checkMore: AsyncValidator[Field, Output]*): Builder[Output] = {

    asyncK(toField)(Kleisli(checkFirst), checkMore.map(Kleisli(_)): _*)
  }

  def asyncK[Field, Output](toField: Subject => Field)(
    checkFirst: AsyncValidatorK[Field, Output],
    checkMore: AsyncValidatorK[Field, Output]*
  ): Builder[Output] = {
    //semigroup for success values in case of multiple validations on a single field
    implicit val outputSemigroup: Semigroup[Output] = Semigroup.instance[Output]((a, _) => a)

    val checkAll: AsyncValidatorK[Field, Output] =
      NonEmptyList(checkFirst, checkMore.toList).reduce(validatorSemigroup[AsyncValidatorK[Field, ?], Output])

    val mapAndCheck: AsyncValidatorK[Subject, Output] = checkAll.local(toField)

    new Builder[Output](
      //memory first to maintain order of errors in case of failure
      (memory, mapAndCheck).parMapN((memory, field) => field :: memory)
    )
  }

  def to[Success]: To[Success] = new To[Success]

  class To[Success] private[vivalidi] {

    def run[SuccessReverseRepr <: HList](
      implicit gen: Generic.Aux[Success, SuccessReverseRepr],
      rev: Reverse.Aux[SuccessRepr, SuccessReverseRepr]
    ): Subject => F[Success] = {
      memory.map(f => gen.from(rev(f)))
    }.run
  }

  private def validatorSemigroup[G[_]: Parallel, T: Semigroup]: Semigroup[G[T]] = (_, _).parMapN(_ |+| _)

}
