package vivalidi
import cats.Show
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.effect.IO
import cats.implicits._
import org.scalatest.concurrent.Eventually
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global //for timer/parallel of IO

class VivalidiTests extends WordSpec with Matchers with Eventually {
  "Validations" should {
    "be parallel" in {
      val sleepLength: FiniteDuration = 1.second
      val Δ                           = 200.millis

      def delayReturnPure[T: Show]: T => IO[ValidatedNel[String, T]] = { t =>
        val sleep = IO.sleep(sleepLength)

        sleep.as(Validated.valid(t))
      }

      val validation: Person => IO[ValidatedNel[String, Person]] = Vivalidi
        .init[Person, ValidatedNel[String, ?], IO]
        .async(_.id)(delayReturnPure, delayReturnPure, delayReturnPure)
        .async(_.name)(delayReturnPure)
        .async(_.age)(delayReturnPure)
        .to[Person]
        .run(_)

      val person = Person(1, "hello", 21)

      eventually {
        validation(person).unsafeRunTimed(sleepLength + Δ) shouldBe Some(person.valid)
      }
    }
  }

  "Single field validations" should {
    "compose errors" in {

      case class UserId(value: Long)

      def failIO[T](error: String): T => IO[ValidatedNel[String, T]] = _ => error.invalidNel.pure[IO]

      val validation: UserId => IO[ValidatedNel[String, UserId]] = Vivalidi
        .init[UserId, ValidatedNel[String, ?], IO]
        .async(_.value)(failIO("oops"), failIO("foo"), failIO("bar"))
        .to[UserId]
        .run(_)

      val person = UserId(1)

      validation(person).unsafeRunSync shouldBe NonEmptyList.of("oops", "foo", "bar").invalid
    }
  }

  "Validations on multiple fields" should {
    "compose errors in correct order" in {
      val validation: Person => IO[ValidatedNel[String, Person]] = Vivalidi
        .init[Person, ValidatedNel[String, ?], IO]
        .async(_.id)(_ => "wrong id".invalidNel[Long].pure[IO])
        .sync(_.name)(_.valid)
        .async(_.age)(_ => "wrong age".invalidNel[Int].pure[IO])
        .to[Person]
        .run(_)

      val person = Person(1, "hello", 21)

      validation(person).unsafeRunSync() shouldBe NonEmptyList.of("wrong id", "wrong age").invalid
    }
  }
}

case class Person(id: Long, name: String, age: Int)
