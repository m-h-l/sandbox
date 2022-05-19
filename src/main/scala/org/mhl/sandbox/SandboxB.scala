package org.mhl.sandbox

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.remote.transport.ActorTransportAdapter.AskTimeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object SandboxB {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val actorSystem: ActorSystem[PingPong] = ActorSystem[PingPong](
      Behaviors.empty,
      name = "root",
      config = config.getConfig("second").withFallback(config.withoutPath("first").withoutPath("third"))
    )

    actorSystem.scheduler.scheduleAtFixedRate(0 seconds, 10 seconds) { () =>

      import akka.actor.typed.scaladsl.AskPattern._
      implicit val scheduler: Scheduler = actorSystem.scheduler

      actorSystem.receptionist.ask[Receptionist.Listing]{ replyTo =>
        Receptionist.find(ServiceKey[PingPong]("foo"), replyTo)
      }
        .map(_.serviceInstances(ServiceKey[PingPong]("foo")).head)
        .map { ref =>
          ref.ask[PingPong] { replyTo =>
            Ping(replyTo)
          }.map { _ =>
            println("*** got pong")
          }
        }
    }


  }
}
