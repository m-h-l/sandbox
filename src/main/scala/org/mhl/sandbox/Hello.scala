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

object Hello extends SandboxActor("Hello"):

  trait Protocol

  case class Greet(whom: String, replyTo: Option[ActorRef[Protocol]]) extends Protocol

  case class Die() extends Protocol

  case class Ack() extends Protocol

  def deploy(actorSystem: ActorSystem[Dispatcher.Protocol])(implicit scheduler: Scheduler): Future[Hello] =
    actorSystem.ask[Dispatcher.Protocol] { replyTo =>
      Dispatcher.Register(Hello.name, Hello.behavior, replyTo) }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case Dispatcher.Retrieved(id, ref) => Hello(ref.asInstanceOf[ActorRef[Hello.Protocol]])
      }

  def behavior: Behavior[Protocol] =
    Behaviors.receive { (context, message) =>
      message match
        case Greet(whom, replyTo) =>
          say(s"Hello $whom")
          replyTo.foreach(_ ! Ack())
        case Die() =>
          say("boom!")
          throw new Exception()
        case _ => ???
      Behaviors.same
    }

class Hello(actor: ActorRef[Hello.Protocol])(implicit scheduler: Scheduler):

  def greet(whom: String): Future[Done] =
    actor.ask[Hello.Protocol] { replyTo => Hello.Greet(whom, Some(replyTo)) }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case Hello.Ack() => Done.done()
      }

  def die(): Unit =
    actor ! Hello.Die()
