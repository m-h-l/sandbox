package org.mhl.sandbox

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object SandboxActor {
  trait Protocol
}


trait SandboxActor[T] {

  val name: String

  def apply(ref: ActorRef[_])(implicit scheduler: Scheduler): T

  def behavior: Behavior[SandboxActor.Protocol]

  def deploy(context: ActorContext[Dispatcher.Protocol]): Future[T] = {

    val ref = context.spawn[SandboxActor.Protocol](behavior, this.name)
    say(s"Spawned ${this.name}")

    context.system.receptionist.ask[Receptionist.Registered] { replyTo =>
      Receptionist.register(ServiceKey[SandboxActor.Protocol](name), ref, replyTo)
    }(Timeout.durationToTimeout(30 seconds), context.system.scheduler).map { response =>
      apply(ref)(context.system.scheduler)
    }
  }

  def say(text: String): Unit = println(s"$name says: $text")

  def residingOn(actorSystem: ActorSystem[Dispatcher.Protocol]): Future[T] =
    actorSystem.receptionist.ask[Receptionist.Listing] { replyTo => Receptionist.find(ServiceKey[SandboxActor.Protocol](this.name), replyTo)
    }(Timeout.durationToTimeout(30 seconds), actorSystem.scheduler).map { response =>
      val instances = response.serviceInstances(ServiceKey[SandboxActor.Protocol](name))
      instances.headOption match {
        case Some(ref) => apply(ref)(actorSystem.scheduler)
        case None => throw new NoSuchElementException()
      }

    }
}
