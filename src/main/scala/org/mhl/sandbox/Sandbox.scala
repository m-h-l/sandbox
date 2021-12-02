package org.mhl.sandbox

import Sandbox.HelloWorld.{Ack, Greet, Protocol}
import akka.Done
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler, SupervisorStrategy}
import akka.util.Timeout
import Sandbox.Dispatcher.{Register, Retrieved}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object Sandbox {

  object Dispatcher {

    trait ChildProtocol

    trait Protocol

    case class Register(id: String, behavior: Behavior[ChildProtocol], replyTo: ActorRef[Protocol]) extends Protocol
    case class Retrieve(id: String, replyTo: ActorRef[Protocol]) extends Protocol
    case class Retrieved(id: String, ref: ActorRef[ChildProtocol]) extends Protocol
    case class NotFound(id: String) extends Protocol

    def default(registered: Map[String, ActorRef[ChildProtocol]]): Behavior[Protocol] = Behaviors.receive { (context, message) =>
      message match {
        case Register(id, behavior, replyTo) => {
          val wrapped = Behaviors.supervise(behavior).onFailure(SupervisorStrategy.restart)
          val ref = context.spawn(wrapped, id)
          replyTo ! Retrieved(id, ref)
          default(registered + (id -> ref))
        }
        case Retrieve(id, replyTo) => {
          registered.get(id) match {
            case Some(ref) => replyTo ! Retrieved(id, ref)
            case None => replyTo ! NotFound(id)
          }
          Behaviors.same
        }
        case _ => ???
      }
    }
  }


  object HelloWorld {

    trait Protocol extends Dispatcher.ChildProtocol

    case class Greet(whom: String, replyTo: Option[ActorRef[Protocol]]) extends Protocol

    case class Die() extends Protocol

    case class Ack() extends Protocol

    def deploy(actorSystem: ActorSystem[Dispatcher.Protocol])(implicit scheduler: Scheduler): Future[HelloWorld] = {
      actorSystem.ask[Dispatcher.Protocol] { replyTo => Register("hello", HelloWorld.default, replyTo) }(Timeout.durationToTimeout(30 seconds), scheduler)
        .map {
          case Retrieved(id, ref) => new HelloWorld(ref)
        }
    }

    def default: Behavior[Dispatcher.ChildProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case Greet(whom, replyTo) => {
          println(s"Hello $whom")
          replyTo.foreach(_ ! Ack())
        }
        case Die() => {
          println("boom!")
          throw new Exception()
        }
        case _ => ???
      }
      Behaviors.same
    }
  }

  class HelloWorld(actor: ActorRef[Protocol])(implicit scheduler: Scheduler) {

    def greet(whom: String): Future[Done] = {

      actor.ask[Protocol] { replyTo => Greet(whom, Some(replyTo)) }(Timeout.durationToTimeout(30 seconds), scheduler)
        .map {
          case Ack() => Done.done()
        }
    }

    def die(): Unit = {
      actor ! HelloWorld.Die()
    }

  }

  def main(args: Array[String]): Unit = {

    val actorSystem: ActorSystem[Dispatcher.Protocol] = ActorSystem[Dispatcher.Protocol](Dispatcher.default(Map.empty), "root")
    val hello = HelloWorld.deploy(actorSystem)(actorSystem.scheduler)
    hello.map{ hl =>
      hl.die()
      hl.greet("from the dead").map(_ => println("confirmed"))
    }

  }
}
