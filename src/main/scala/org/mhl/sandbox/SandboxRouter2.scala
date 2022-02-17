package org.mhl.sandbox

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, Routers}
import akka.actor.typed.{ActorRef, Behavior, Scheduler, SupervisorStrategy}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.reflect.ClassTag


class SandboxRouter2[T : ClassTag](poolSize: Int, routedActor: SandboxActor[T]) extends SandboxActor[T] {

  override val name: String = s"PoolRouter-${routedActor.name}"

  override def deploy(context: ActorContext[SandboxActor.Protocol]): Future[T] = {

    val ref = context.spawn[SandboxActor.Protocol](initialBehavior, this.name)
    say(s"Spawned ${this.name}")

    context.system.receptionist.ask[Receptionist.Registered] { replyTo =>
      Receptionist.register(ServiceKey[SandboxActor.Protocol](routedActor.name), ref, replyTo)
    }(Timeout.durationToTimeout(30 seconds), context.system.scheduler).map { response =>
      apply(ref)(context.system.scheduler)
    }
  }

  override def apply(ref: ActorRef[SandboxActor.Protocol])(implicit scheduler: Scheduler): T = routedActor.apply(ref)

  override def initialBehavior: Behavior[SandboxActor.Protocol] = {
      Routers.pool[SandboxActor.Protocol](poolSize) {
        Behaviors.supervise(routedActor.initialBehavior).onFailure[Exception](SupervisorStrategy.restart)
      }
        .withRoundRobinRouting()
        .behavior
  }
}
