package org.mhl.sandbox

import akka.Done
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import org.mhl.sandbox

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object Hello extends SandboxActor[Hello] {

  val name = "Hello"

  def apply(actor: ActorRef[SandboxActor.Protocol])(implicit scheduler: Scheduler): Hello = new Hello(actor)

  def initialBehavior: Behavior[SandboxActor.Protocol] = Behaviors.receiveMessage {
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

  case class Greet(whom: String, override val replyTo: Option[ActorRef[SandboxActor.Protocol]]) extends SandboxActor.Request(replyTo) {
    override def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): Greet = Greet(whom, Some(actorRef))
  }

  case class Die() extends SandboxActor.Request(None) {
    override def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): Die = Die()
  }

  case class Ack() extends SandboxActor.Response
}

class Hello(actor: ActorRef[SandboxActor.Protocol])(implicit scheduler: Scheduler) {

  def greet(whom: String): Future[Done] = {
    actor.ask[SandboxActor.Protocol] { replyTo => Hello.Greet(whom, Some(replyTo)) }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case Hello.Ack() => Done.done()
      }
  }

  def die(): Unit = {
    actor ! Hello.Die()
  }
}
