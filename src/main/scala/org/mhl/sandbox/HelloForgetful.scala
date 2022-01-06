package org.mhl.sandbox

import akka.Done
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object HelloForgetful extends SandboxActor[HelloForgetful] {

  val name = "HelloForgetful"

  override def apply(ref: ActorRef[_])(implicit scheduler: Scheduler): HelloForgetful = new HelloForgetful(ref.asInstanceOf[ActorRef[HelloForgetful.Protocol]])

  override def behavior: Behavior[SandboxActor.Protocol] = behavior(0, Set.empty).asInstanceOf[Behavior[SandboxActor.Protocol]]

  def behavior(greetCount: Int, greeted: Set[String]): Behavior[HelloForgetful.Protocol] = {
    Behaviors.supervise {
      Behaviors.receive[HelloForgetful.Protocol] { (context, message) =>
        message match {
          case Greet(whom, replyTo) => {
            val updatedGreetCount = greetCount + 1
            val updatedGreeted = greeted + whom
            say(s"Hello $whom")
            say(s"Until now, I have greeted $updatedGreetCount times. ${updatedGreeted.mkString(", ")} have been greeted.")
            replyTo.foreach(_ ! Ack())
            behavior(updatedGreetCount, updatedGreeted)
          }
          case Die() => {
            say("boom!")
            throw new Exception()
          }
          case _ => ???
        }
      }
    }.onFailure(SupervisorStrategy.restart)
  }

  trait Protocol extends SandboxActor.Protocol

  case class Greet(whom: String, replyTo: Option[ActorRef[Protocol]]) extends Protocol

  case class Die() extends Protocol

  case class Ack() extends Protocol
}

class HelloForgetful(actor: ActorRef[HelloForgetful.Protocol])(implicit scheduler: Scheduler) {

  def greet(whom: String): Future[Done] =
    actor.ask[HelloForgetful.Protocol] { replyTo => HelloForgetful.Greet(whom, Some(replyTo)) }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case HelloForgetful.Ack() => Done.done()
      }

  def die(): Unit =
    actor ! HelloForgetful.Die()
}
