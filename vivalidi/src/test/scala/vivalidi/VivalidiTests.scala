package vivalidi

import cats.Show
import cats.data._
import cats.effect.{ContextShift, IO, Timer}
import cats.effect.laws.util.TestContext
import cats.implicits._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.language.higherKinds

class VivalidiTests extends WordSpec with Matchers {
  type EitherNelT[F[_], E, T] = EitherT[F, NonEmptyList[E], T]

  "Parallel validation" should {
    "be parallel" in {
      implicit val tc: TestContext = TestContext()

      implicit val contextShift: ContextShift[IO] = IO.contextShift(tc)
      implicit val timer: Timer[IO] = tc.timer[IO]

      val sleepLength: FiniteDuration = 1.second

      def delayReturnPure[T: Show, E]: T => EitherT[IO, E, T] = { t =>
        val debug = false

        val sleep = IO.sleep(sleepLength)

        val action =
          if (debug) IO(println(show"starting $t")) *> sleep <* IO(println(show"finishing $t"))
          else sleep

        EitherT(action.as(t.asRight[E]))
      }

      val validation = Vivalidi[Person, EitherT[IO, Unit, ?]].init
        .async(_.id)(delayReturnPure, delayReturnPure, delayReturnPure)
        .async(_.name)(delayReturnPure)
        .async(_.age)(delayReturnPure)
        .to[Person]
        .run

      val person = Person(1, "hello", 21)


      val future = validation(person).value.unsafeToFuture()

      tc.tick(sleepLength)
      future.value.get.get shouldBe person.asRight
    }
  }

  "Single field validations" should {
    "compose errors" in {

      case class UserId(value: Long)

      def failIO[T](error: String): T => EitherNelT[IO, String, T] =
        _ => error.leftNel[T].liftTo[EitherNelT[IO, String, ?]]

      val validation: UserId => EitherNelT[IO, String, UserId] = Vivalidi[UserId, EitherNelT[IO, String, ?]].init
        .async(_.value)(failIO("oops"), failIO("foo"), failIO("bar"))
        .to[UserId]
        .run

      val person = UserId(1)

      validation(person).value.unsafeRunSync shouldBe NonEmptyList.of("oops", "foo", "bar").asLeft
    }
  }

  "Validations on multiple fields" should {
    "compose errors in correct order" in {
      type E[A] = EitherNelT[IO, String, A]

      val validation: Person => EitherNelT[IO, String, Person] = Vivalidi[Person, E].init
        .sync(_.id)(_ => "wrong id".leftNel[Long])
        .just(_.name)
        .async(_.age)(_ => "wrong age".leftNel[Int].liftTo[E])
        .to[Person]
        .run

      val person = Person(1, "hello", 21)

      validation(person).value.unsafeRunSync() shouldBe NonEmptyList.of("wrong id", "wrong age").asLeft
    }
  }

  "Kleisli validations on multiple fields" should {
    "compose errors in correct order" in {
      type E[A] = EitherNelT[IO, String, A]

      val validation: Person => EitherNelT[IO, String, Person] = Vivalidi[Person, E].init
        .sync(_.id)(_ => "wrong id".leftNel[Long])
        .just(_.name)
        .asyncK(_.age)(Kleisli.liftF("wrong age".leftNel[Int].liftTo[E]))
        .to[Person]
        .run

      val person = Person(1, "hello", 21)

      validation(person).value.unsafeRunSync() shouldBe NonEmptyList.of("wrong id", "wrong age").asLeft
    }
  }
}

case class Person(id: Long, name: String, age: Int)
