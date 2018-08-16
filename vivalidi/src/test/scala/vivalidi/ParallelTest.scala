package vivalidi
import cats.Show
import cats.data.{Validated, ValidatedNel}
import cats.effect.IO
import cats.implicits._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

case class Person(id: Long, name: String, age: Int)

class ParallelTest extends WordSpec with Matchers {
  "Validations" should {
    "be parallel" in {
      val sleepLength = 1.second
      val Δ           = 500.millis

      import scala.concurrent.ExecutionContext.Implicits.global

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

      validation(person).unsafeRunTimed(sleepLength + Δ) shouldBe Some(person.valid)
    }
  }
}
