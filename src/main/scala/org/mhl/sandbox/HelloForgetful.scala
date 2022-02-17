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

  override def apply(ref: ActorRef[SandboxActor.Protocol])(implicit scheduler: Scheduler): HelloForgetful = new HelloForgetful(ref)

  override def initialBehavior: Behavior[SandboxActor.Protocol] = Behaviors.supervise {
    behavior(0, Set.empty)
  }.onFailure(SupervisorStrategy.restart)

  def behavior(greetCount: Int, greeted: Set[String]): Behavior[SandboxActor.Protocol] = {
    Behaviors.receive[SandboxActor.Protocol] { (context, message) =>
      message match {
        case Greet(whom, replyTo) => {
          val updatedGreetCount = greetCount + 1
          val updatedGreeted = greeted + whom
          say(s"Hello $whom")
          say(s"Until now, I have greeted $updatedGreetCount times. ${updatedGreeted.mkString(", ")} have been greeted.")
          replyTo.foreach(_ ! Ack())
          behavior(updatedGreetCount, updatedGreeted)
        }
        case Die(replyTo) => {
          say("boom!")
          throw new Exception()
        }
        case _ => ???
      }
    }
  }

  case class Greet(whom: String, override val replyTo: Option[ActorRef[SandboxActor.Protocol]]) extends SandboxActor.Request(replyTo) {
    override def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): Greet = {
      Greet(whom, Some(actorRef))
    }
  }

  case class Die(override val replyTo: Option[ActorRef[SandboxActor.Protocol]]) extends SandboxActor.Request(None) {
    override def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): Die = {
      Die(Some(actorRef))
    }
  }

  case class Ack() extends SandboxActor.Response
}

class HelloForgetful(actor: ActorRef[SandboxActor.Protocol])(implicit scheduler: Scheduler) {

  def greet(whom: String): Future[Done] =
    actor.ask[SandboxActor.Protocol] { replyTo => HelloForgetful.Greet(whom, Some(replyTo)) }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case HelloForgetful.Ack() => Done.done()
      }

  def die(): Unit =
    actor ! HelloForgetful.Die(None)
}
