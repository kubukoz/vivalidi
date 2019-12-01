package vivalidi

import cats.data._
import cats.effect.laws.util.TestContext
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._

import scala.concurrent.duration._
import scala.language.higherKinds
import cats.ApplicativeError
import cats.Parallel
import cats.effect.Sync
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class VivalidiTests extends AsyncWordSpec with Matchers {
  type EitherNelT[F[_], E, T] = EitherT[F, NonEmptyList[E], T]

  type FailNelString[F[_]] = ApplicativeError[F, NonEmptyList[String]]

  val tc: TestContext = TestContext()

  implicit val contextShift: ContextShift[IO] = tc.contextShift(IO.ioEffect)
  implicit val timer: Timer[IO]               = tc.timer[IO]

  implicit def eitherTTimer[E]: Timer[EitherT[IO, E, ?]] = timer.mapK(EitherT.liftK)

  "Parallel validation" should {
    "be parallel" in {

      val sleepLength: FiniteDuration = 1.second

      def validation[F[_]: Parallel: FailNelString: Timer: Sync] = {
        def delayReturnPure[T]: T => F[T] = { t =>
          val debug = false

          val sleep = Timer[F].sleep(sleepLength)

          val action =
            if (debug) Sync[F].delay(println(s"starting $t")) >> sleep >> Sync[F].delay(println(s"finishing $t"))
            else sleep

          action >> t.pure(Sync[F])
        }

        Vivalidi[Person, F]
          .init(Parallel[F], implicitly[FailNelString[F]])
          .async(_.id)(delayReturnPure, delayReturnPure, delayReturnPure)
          .async(_.name)(delayReturnPure)
          .async(_.age)(delayReturnPure)
          .to[Person]
          .run
      }

      val person = Person(1, "hello", 21)

      val future = validation[EitherNelT[IO, String, ?]].apply(person).value.timeout(2.seconds).unsafeToFuture()

      tc.tick(sleepLength)

      future.map(_ shouldBe person.asRight)
    }
  }

  "Single field validations" should {
    "compose errors" in {

      case class UserId(value: Long)

      def validation[F[_]: Parallel: FailNelString]: UserId => F[UserId] = {
        def failIO[T](error: String): T => F[T] =
          _ => error.leftNel[T].liftTo[F]

        Vivalidi[UserId, F].init.async(_.value)(failIO("oops"), failIO("foo"), failIO("bar")).to[UserId].run
      }

      val person = UserId(1)

      val future = validation[EitherNelT[IO, String, ?]].apply(person).value.timeout(2.seconds).unsafeToFuture()

      tc.tick()

      future.map(_ shouldBe NonEmptyList.of("oops", "foo", "bar").asLeft)

    }
  }

  "Validations on multiple fields" should {
    "compose errors in correct order" in {

      def validation[E[_]: Parallel: FailNelString]: Person => E[Person] =
        Vivalidi[Person, E].init
          .sync(_.id)(_ => "wrong id".leftNel[Long])
          .just(_.name)
          .async(_.age)(_ => "wrong age".leftNel[Int].liftTo[E])
          .to[Person]
          .run

      val person = Person(1, "hello", 21)

      val future = validation[EitherNelT[IO, String, ?]].apply(person).value.timeout(2.seconds).unsafeToFuture()

      tc.tick()

      future.map(_ shouldBe NonEmptyList.of("wrong id", "wrong age").asLeft)
    }
  }

  "Kleisli validations on multiple fields" should {
    "compose errors in correct order" in {
      def validation[E[_]: Parallel: FailNelString]: Person => E[Person] =
        Vivalidi[Person, E].init
          .sync(_.id)(_ => "wrong id".leftNel[Long])
          .just(_.name)
          .asyncK(_.age)(Kleisli.liftF("wrong age".leftNel[Int].liftTo[E]))
          .to[Person]
          .run

      val person = Person(1, "hello", 21)

      val future = validation[EitherNelT[IO, String, ?]].apply(person).value.timeout(2.seconds).unsafeToFuture()

      tc.tick()

      future.map(_ shouldBe NonEmptyList.of("wrong id", "wrong age").asLeft)
    }
  }
}

case class Person(id: Long, name: String, age: Int)
