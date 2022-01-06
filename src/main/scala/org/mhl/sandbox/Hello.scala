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

object Hello extends SandboxActor[Hello] {

  val name = "Hello"

  def apply(actor: ActorRef[_])(implicit scheduler: Scheduler) = new Hello(actor.asInstanceOf[ActorRef[Hello.Protocol]])

  def behavior: Behavior[SandboxActor.Protocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case Greet(whom, replyTo) =>
          say(s"Hello $whom")
          replyTo.foreach(_ ! Ack())
          Behaviors.same
        case Die() =>
          say("boom!")
          throw new Exception()
        case _ => ???
          Behaviors.same
      }
    }

  trait Protocol extends SandboxActor.Protocol

  case class Greet(whom: String, replyTo: Option[ActorRef[Protocol]]) extends Protocol

  case class Die() extends Protocol

  case class Ack() extends Protocol
}

class Hello(actor: ActorRef[Hello.Protocol])(implicit scheduler: Scheduler) {

  def greet(whom: String): Future[Done] = {
    actor.ask[Hello.Protocol] { replyTo => Hello.Greet(whom, Some(replyTo)) }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case Hello.Ack() => Done.done()
      }
  }

  def die(): Unit = {
    actor ! Hello.Die()
  }
}
