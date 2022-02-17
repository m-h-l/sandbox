package org.mhl.sandbox

import akka.Done
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}
import akka.util.Timeout
import org.mhl.sandbox

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object HelloDurableState extends SandboxActor[HelloDurableState] {

  val name = "HelloDurableState"

  override def apply(ref: ActorRef[SandboxActor.Protocol])(implicit scheduler: Scheduler): HelloDurableState = new HelloDurableState(ref)

  def initialBehavior(): Behavior[SandboxActor.Protocol] = {
    DurableStateBehavior[Command, State](
      persistenceId = PersistenceId.ofUniqueId(this.name),
      emptyState = State(0, Set.empty),
      commandHandler = { (state, command) =>
        command match {
          case Greet(whom, replyTo) => {
            say(s"Hello $whom")
            say(s"Until now, I have greeted ${state.greetCount} times. ${state.greeted.mkString(", ")} have been greeted.")
            replyTo.foreach(_ ! Ack())
            Effect.persist(State(state.greetCount + 1, state.greeted + whom))
          }
          case Die()
          => {
            say("boom!")
            throw new Exception()
          }
          case msg => {
            say(msg.toString)
            Effect.none
          }
        }
      }
    ).asInstanceOf[Behavior[SandboxActor.Protocol]]
  }

  trait Command

  final case class State(greetCount: Int, greeted: Set[String]) extends Serializable

  case class Greet(whom: String, override val replyTo: Option[ActorRef[SandboxActor.Protocol]]) extends SandboxActor.Request(replyTo) with Command {
    override def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): Greet = {
      Greet(whom, Some(actorRef))
    }
  }

  case class Die() extends SandboxActor.Request(None) with Command {
    override def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): Die = Die()
  }

  case class Ack() extends SandboxActor.Response
}

class HelloDurableState(actor: ActorRef[SandboxActor.Protocol])(implicit scheduler: Scheduler) {
  def greet(whom: String): Future[Done] = {
    actor.ask[SandboxActor.Protocol] {
      replyTo => HelloDurableState.Greet(whom, Some(replyTo))
    }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case HelloDurableState.Ack() => Done.done()
      }
  }

  def die(): Unit = actor ! HelloDurableState.Die()
}
