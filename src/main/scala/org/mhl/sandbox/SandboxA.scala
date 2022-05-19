package org.mhl.sandbox

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.remote.transport.ActorTransportAdapter.AskTimeout
import com.github.nscala_time.time.Imports.{DateTime, richReadableInstant}
import com.typesafe.config.ConfigFactory
import org.joda.time.PeriodType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

object SandboxA {



  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val actorSystem: ActorSystem[PingPong] = ActorSystem[PingPong](
      Behaviors.setup[PingPong] { ctx =>
        val actor = ctx.spawnAnonymous(
          Behaviors.receiveMessage[PingPong] {
            case Ping(replyTo) => {
              println(s"$replyTo says Ping?")
              replyTo.tell(Pong())
              Behaviors.same
            }
            case Pong() => {
              println("got Pong!")
              Behaviors.same
            }
          }
        )
        ctx.system.receptionist.tell(Receptionist.register(ServiceKey[PingPong]("foo"), actor))
        Behaviors.empty
      },
      name = "root",
      config = config.getConfig("first").withFallback(config.withoutPath("second").withoutPath("third"))
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
