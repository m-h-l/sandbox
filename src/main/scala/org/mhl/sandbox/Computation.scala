package org.mhl.sandbox

import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import com.github.nscala_time.time.Imports.{DateTime, richReadableInstant}
import org.joda.time.PeriodType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object Computation extends SandboxActor[Computation] {

  val name = "Computation"

  override def apply(ref: ActorRef[_])(implicit scheduler: Scheduler): Computation = new Computation(ref.asInstanceOf[ActorRef[Computation.Protocol]])

  def behavior: Behavior[SandboxActor.Protocol] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Compute(n, replyTo) => {
          val startTime = DateTime.now()
          val result = factorial(n)
          val duration = (startTime to DateTime.now()).toPeriod(PeriodType.millis()).getMillis
          say(s"Finished. Took ${duration} ms")
          replyTo.foreach(_ ! Result(result))
          Behaviors.same
        }
        case _ => ???
      }
    }
  }

  private def factorial(n: Int): BigInt = {
    n match {
      case 0 => 1
      case n if n < 0 => throw new IndexOutOfBoundsException()
      case n => n * factorial(n - 1)
    }
  }

  trait Protocol extends SandboxActor.Protocol

  case class Compute(n: Int, replyTo: Option[ActorRef[Protocol]]) extends Protocol

  case class Result(int: BigInt) extends Protocol
}

class Computation(actor: ActorRef[Computation.Protocol])(implicit scheduler: Scheduler) {

  def compute(n: Int): Future[BigInt] =
    actor.ask[Computation.Protocol] { replyTo => Computation.Compute(n, Some(replyTo)) }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case Computation.Result(n) => n
      }
}