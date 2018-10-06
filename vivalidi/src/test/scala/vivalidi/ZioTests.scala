package vivalidi

import cats.implicits._
import cats.data.NonEmptyList
import cats.kernel.Semigroup
import cats.{Applicative, Monad, MonadError, Parallel, ~>}
import org.scalatest.{Matchers, WordSpec}
import scalaz.zio.{RTS, IO => ZIO}

class ParZIO[+E, +T](val value: ZIO[E, T]) extends AnyVal

object ZIOInstances {
  implicit def monadZIO[E]: MonadError[ZIO[E, ?], E] = new MonadError[ZIO[E, ?], E] {
    override def flatMap[A, B](fa: ZIO[E, A])(f: A => ZIO[E, B]): ZIO[E, B] = fa.flatMap(f)
    override def tailRecM[A, B](a: A)(f: A => ZIO[E, Either[A, B]]): ZIO[E, B] = f(a).flatMap {
      case Left(a)  => tailRecM(a)(f)
      case Right(b) => pure(b)
    }
    override def pure[A](x: A): ZIO[E, A]                                        = ZIO.now(x)
    override def raiseError[A](e: E): ZIO[E, A]                                  = ZIO.fail(e)
    override def handleErrorWith[A](fa: ZIO[E, A])(f: E => ZIO[E, A]): ZIO[E, A] = fa.catchAll(f)
  }

  implicit def applicativeZIOPar[E: Semigroup]: Applicative[ParZIO[E, ?]] = new Applicative[ParZIO[E, ?]] {
    override def pure[A](x: A): ParZIO[E, A] = new ParZIO(ZIO.now(x))
    override def ap[A, B](ff: ParZIO[E, A => B])(fa: ParZIO[E, A]): ParZIO[E, B] =
      new ParZIO(
        ff.value.attempt
          .parWith(fa.value.attempt){
            case (Left(e), Left(ee))   => Left(Semigroup[E].combine(e, ee))
            case (Left(e), _)          => Left(e)
            case (_, Left(e))          => Left(e)
            case (Right(fa), Right(a)) => Right(fa(a))
          }
          .flatMap(_.liftTo[ZIO[E, ?]]))
  }

  implicit def parallel[E: Semigroup]: Parallel[ZIO[E, ?], ParZIO[E, ?]] = new Parallel[ZIO[E, ?], ParZIO[E, ?]] {
    override def applicative: Applicative[ParZIO[E, ?]]  = applicativeZIOPar[E]
    override def monad: Monad[ZIO[E, ?]]                 = monadZIO[E]
    override def sequential: ~>[ParZIO[E, ?], ZIO[E, ?]] = λ[ParZIO[E, ?] ~> ZIO[E, ?]](_.value)
    override def parallel: ~>[ZIO[E, ?], ParZIO[E, ?]]   = λ[ZIO[E, ?] ~> ParZIO[E, ?]](new ParZIO(_))
  }
}

class ZioTests extends WordSpec with Matchers {

  "ZIO" should {
    "just work" in {
      import ZIOInstances._
      import cats.implicits._

      type E[A] = ZIO[NonEmptyList[String], A]

      val validation: Person => E[Person] = Vivalidi[Person, E].init
        .sync(_.id)(_ => "wrong id".leftNel[Long])
        .just(_.name)
        .async(_.age)(_ => "wrong age".leftNel[Int].liftTo[E])
        .to[Person]
        .run

      val person = Person(1, "hello", 21)

      val fun = validation(person)

      val rts = new RTS {}

      val prog = fun.attempt.flatMap { result =>
        ZIO
          .sync(result shouldBe NonEmptyList.of("wrong id", "wrong age").asLeft)
      }

      rts.unsafeRun(prog)
    }
  }
}
