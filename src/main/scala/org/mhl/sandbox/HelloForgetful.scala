package org.mhl.sandbox

import akka.Done
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object HelloForgetful extends SandboxActor("HelloForgetful") {

  def deploy(actorSystem: ActorSystem[Dispatcher.Protocol])(implicit scheduler: Scheduler): Future[HelloForgetful] =
    actorSystem.ask[Dispatcher.Protocol] { replyTo =>
      Dispatcher.Register("helloStateful", HelloForgetful.behavior(0, Set.empty), replyTo)
    }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case Dispatcher.Retrieved(id, ref) => new HelloForgetful(ref.asInstanceOf[ActorRef[HelloForgetful.Protocol]])
      }

  def behavior(greetCount: Int, greeted: Set[String]): Behavior[Protocol] = {
    Behaviors.receive { (context, message) =>
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
  }

  trait Protocol

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
