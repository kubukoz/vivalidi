package vivalidi.builder

import cats.data.NonEmptyList
import cats.implicits._
import cats.kernel.Semigroup
import cats.temp.par._
import cats.{Applicative, Monad}
import shapeless.ops.hlist.Reverse
import shapeless.{::, Generic, HList}
import vivalidi.syntax

import scala.language.higherKinds

private[vivalidi] final class VivalidiBuilder[Subject, Errors[_], SuccessRepr <: HList, F[_]](
  memory: Subject => F[Errors[SuccessRepr]])(implicit F: Par[F], E: Applicative[Errors]) {
  implicit val monF: Monad[F] = F.parallel.monad

  type SyncValidator[I, O]  = I => Errors[O]
  type AsyncValidator[I, O] = I => F[Errors[O]]

  def pure[Field](value: Field): VivalidiBuilder[Subject, Errors, Field :: SuccessRepr, F] =
    just(Function.const(value))

  def just[Field](toField: Subject => Field): VivalidiBuilder[Subject, Errors, Field :: SuccessRepr, F] =
    sync(toField)(E.pure)

  def sync[Field, Output](toField: Subject => Field)(
    checkFirst: SyncValidator[Field, Output],
    checkMore: SyncValidator[Field, Output]*): VivalidiBuilder[Subject, Errors, Output :: SuccessRepr, F] = {
    import syntax.validators._

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

  class To[Success] private[vivalidi] {

    def run[SuccessReverseRepr <: HList](subject: Subject)(
      implicit gen: Generic.Aux[Success, SuccessReverseRepr],
      rev: Reverse.Aux[SuccessRepr, SuccessReverseRepr]): F[Errors[Success]] = {
      applicativeStack.map(memory)(f => gen.from(rev(f)))(subject)
    }
  }

  private def applicativeStackT[T]: Applicative[λ[A => T => F[Errors[A]]]] =
    Applicative[T => ?].compose[F].compose[Errors]

  private val applicativeStack = applicativeStackT[Subject]

  private type AsyncParValidator[I, O] = I => F.ParAux[Errors[O]]

  private def validatorSemigroup[I, T: Semigroup]
    : Semigroup[AsyncValidator[I, T]] = { (a, b) => i => (a(i), b(i)).parMapN((aE, bE) => (aE, bE).mapN(Semigroup[T].combine))
  }
}
