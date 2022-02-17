package org.mhl.sandbox

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, MailboxSelector, Scheduler}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.reflect.ClassTag

object SandboxActor {
  sealed trait Protocol
  abstract class Request(val replyTo: Option[ActorRef[SandboxActor.Response]]) extends Protocol {
    def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): Request
  }
  abstract class Response() extends Protocol
}


abstract class SandboxActor[T : ClassTag] {

  val name: String

  def apply(ref: ActorRef[SandboxActor.Protocol])(implicit scheduler: Scheduler): T

  def initialBehavior: Behavior[SandboxActor.Protocol]

  def deploy(context: ActorContext[SandboxActor.Protocol]): Future[T] = {

    val ref = context.spawn[SandboxActor.Protocol](initialBehavior, this.name, MailboxSelector.bounded(10))
    say(s"Spawned ${this.name}")

    context.system.receptionist.ask[Receptionist.Registered] { replyTo =>
      Receptionist.register(ServiceKey[SandboxActor.Protocol](name), ref, replyTo)
    }(Timeout.durationToTimeout(30 seconds), context.system.scheduler).map { response =>
      apply(ref)(context.system.scheduler)
    }
  }

  def say(text: String): Unit = println(s"$name says: $text")

  def getAll(actorSystem: ActorSystem[SandboxActor.Protocol]): Future[Set[T]] =
    actorSystem.receptionist.ask[Receptionist.Listing] { replyTo => Receptionist.find(ServiceKey[SandboxActor.Protocol](this.name), replyTo)
    }(Timeout.durationToTimeout(30 seconds), actorSystem.scheduler).map { response =>
      val instances = response.serviceInstances(ServiceKey[SandboxActor.Protocol](name))
      if (instances.nonEmpty)
        instances.map(apply(_)(actorSystem.scheduler))
      else {
        throw new NoSuchElementException()
      }

    }

  def getSingle(actorSystem: ActorSystem[SandboxActor.Protocol]): Future[T] = {
    getAll(actorSystem)
      .map(_.head)
  }
}
