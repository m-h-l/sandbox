package org.mhl.sandbox

import akka.Done
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object HelloEventSourced extends SandboxActor[HelloEventSourced] {

  val name = "HelloEventSourced"

  val commandHandler: (State, Command) => Effect[Event, State] = { (state, command) =>
    command match {
      case Greet(whom, replyTo) => {
        say(s"Hello $whom")
        say(s"Until now, I have greeted ${state.greetCount} times. ${state.greeted.mkString(", ")} have been greeted.")
        replyTo.foreach(_ ! Ack())
        Effect.persist(Greeted(whom))
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
  val eventHandler: (State, Event) => State = { (state, event) => {
    event match {
      case Greeted(whom) => {
        State(state.greetCount + 1, state.greeted + whom)
      }
      case event => {
        say(s"Unknown event ${event.toString}")
        state
      }
    }
  }
  }

  override def apply(ref: ActorRef[SandboxActor.Protocol])(implicit scheduler: Scheduler): HelloEventSourced = new HelloEventSourced(ref.asInstanceOf[ActorRef[HelloEventSourced.Command]])

  def initialBehavior: Behavior[SandboxActor.Protocol] = {
    Behaviors.supervise {
      Behaviors.setup[HelloEventSourced.Command] { context =>
        EventSourcedBehavior[Command, Event, State](
          persistenceId = PersistenceId.ofUniqueId(this.name),
          emptyState = HelloEventSourced.State(0, Set.empty),
          commandHandler = commandHandler,
          eventHandler = eventHandler
        )
      }
    }.onFailure(SupervisorStrategy.restart)
      .asInstanceOf[Behavior[SandboxActor.Protocol]]
  }

  trait Event

  abstract class Command(replyTo: Option[ActorRef[SandboxActor.Protocol]]) extends SandboxActor.Request(replyTo)

  case class Greeted(whom: String) extends Event

  case class Greet(whom: String, override val replyTo: Option[ActorRef[SandboxActor.Protocol]]) extends Command(replyTo) {
    override def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): Command = {
      Greet(whom, Some(actorRef))
    }
  }
  case class Die() extends Command(None) {
    override def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): Command = this
  }

  case class Ack() extends Command(None) {
    override def setReplyTo(actorRef: ActorRef[SandboxActor.Protocol]): Command = this
  }

  final case class State(val greetCount: Int, val greeted: Set[String])


}

class HelloEventSourced(actor: ActorRef[HelloEventSourced.Command])(implicit scheduler: Scheduler) {

  def greet(whom: String): Future[Done] = {
    actor.ask[SandboxActor.Protocol] {
      replyTo => HelloEventSourced.Greet(whom, Some(replyTo))
    }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case HelloEventSourced.Ack() => Done.done()
      }
  }

  def die(): Unit = actor ! HelloEventSourced.Die()
}