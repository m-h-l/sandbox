package org.mhl.sandbox

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Behavior, MailboxSelector}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object MyBehaviours {

  private object Bounce {
    case class GetState(override val replyTo: Option[ActorRef[SandboxActor.Protocol]]) extends SandboxActor.Request(replyTo) {
      override def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): SandboxActor.Request = GetState(Some(actorRef))
    }

    case class Done() extends SandboxActor.Request(None) {
      override def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): SandboxActor.Request = this
    }

    case class Busy() extends SandboxActor.Response

    case class State(busy: Boolean) extends SandboxActor.Response

    case class Died() extends SandboxActor.Response

    def apply(wrappedBehavior: Behavior[SandboxActor.Protocol]): Behavior[SandboxActor.Protocol] = {

      def process(ctx: ActorContext[SandboxActor.Protocol], request: SandboxActor.Request, worker: Option[ActorRef[SandboxActor.Protocol]]) = {
        val ref = worker.getOrElse {
          ctx.spawnAnonymous[SandboxActor.Protocol](
            wrappedBehavior,
            MailboxSelector.bounded(10)
          )
        }
        ref.ask[SandboxActor.Protocol] { reply =>
          request.setReplyTo(reply)
        }(Timeout.durationToTimeout(30 seconds), ctx.system.scheduler)
          .map { response =>
            request.replyTo.foreach(_ ! response.asInstanceOf[SandboxActor.Response])
            ctx.self ! Done()
          }
        ctx.watchWith(ref, Died())
        ref
      }

      def internal(busy: Boolean, worker: Option[ActorRef[SandboxActor.Protocol]]): Behavior[SandboxActor.Protocol] = {
        Behaviors.receive {
          case (_, Died()) => {
            internal(busy = false, None)
          }
          case (_, GetState(replyTo)) => {
            replyTo.foreach(_ ! State(busy))
            Behaviors.same
          }
          case (_, Done()) => {
            internal(busy = false, worker)
          }
          case (ctx, rq: SandboxActor.Request) if busy => {
            if (ctx.children.nonEmpty) {
              rq.replyTo.foreach(_ ! Busy())
              Behaviors.same
            } else {
              val ref = process(ctx, rq, worker)
              internal(busy = true, Some(ref))
            }

          }
          case (ctx, rq: SandboxActor.Request) if !busy => {
            val ref = process(ctx, rq, worker)
            internal(busy = true, Some(ref))
          }
          case foo => {
            println(foo)
            Behaviors.same
          }
        }
      }
      internal(busy = false, None)
    }

  }

  def bounce(wrappedBehavior: Behavior[SandboxActor.Protocol]): Behavior[SandboxActor.Protocol] = Bounce.apply(wrappedBehavior)

}

