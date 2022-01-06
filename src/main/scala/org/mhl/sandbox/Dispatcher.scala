package org.mhl.sandbox

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object Dispatcher {
  def behavior(registered: Map[String, ActorRef[_]]): Behavior[Protocol] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Register(id, ref, replyTo) => {
          replyTo ! Retrieved(id, ref)
          println(s"Dispatcher: Registered $id")
          Dispatcher.behavior(registered + (id -> ref))
        }
        case Retrieve(id, replyTo) => registered.get(id) match {
          case Some(ref) => {
            replyTo ! Retrieved(id, ref)
            println(s"Dispatcher: Retrieving $id")
            Behaviors.same
          }
          case None =>
            replyTo ! NotFound(id)
            Behaviors.same
        }
        case _ => ???
      }
    }
  }

  trait Protocol extends Serializable

  case class Register(id: String, ref: ActorRef[_], replyTo: ActorRef[Protocol]) extends Protocol

  case class Retrieve(id: String, replyTo: ActorRef[Protocol]) extends Protocol

  case class Retrieved(id: String, ref: ActorRef[_]) extends Protocol

  case class NotFound(id: String) extends Protocol
}


