package org.mhl.sandbox

import akka.Done
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}
import akka.util.Timeout
import org.mhl.sandbox

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object HelloDurableState extends SandboxActor("HelloDurableState") {

  def deploy(actorSystem: ActorSystem[Dispatcher.Protocol])(implicit scheduler: Scheduler): Future[HelloDurableState] = {
    actorSystem.ask[Dispatcher.Protocol] { replyTo =>
      Dispatcher.Register("helloStateful", HelloDurableState.behavior(), replyTo)
    }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case Dispatcher.Retrieved(id, ref) => new HelloDurableState(ref.asInstanceOf[ActorRef[sandbox.HelloDurableState.Protocol]])
      }
  }

  def behavior(): DurableStateBehavior[Protocol, State] = {
    DurableStateBehavior[Protocol, State](
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
    )
  }

  trait Protocol extends Serializable

  final case class State(greetCount: Int, greeted: Set[String]) extends Serializable

  case class Greet(whom: String, replyTo: Option[ActorRef[Protocol]]) extends Protocol

  case class Die() extends Protocol

  case class Ack() extends Protocol
}

class HelloDurableState(actor: ActorRef[sandbox.HelloDurableState.Protocol])(implicit scheduler: Scheduler) {
  def greet(whom: String): Future[Done] = {
    actor.ask[HelloDurableState.Protocol] {
      replyTo => HelloDurableState.Greet(whom, Some(replyTo))
    }(Timeout.durationToTimeout(30 seconds), scheduler)
      .map {
        case HelloDurableState.Ack() => Done.done()
      }
  }

  def die(): Unit = actor ! HelloDurableState.Die()
}
