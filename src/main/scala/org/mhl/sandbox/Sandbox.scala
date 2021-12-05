package org.mhl.sandbox

import akka.Done
import akka.actor.typed.ActorSystem

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Sandbox:

  def main(args: Array[String]): Unit =

    val actorSystem: ActorSystem[Dispatcher.Protocol] = ActorSystem[Dispatcher.Protocol](Dispatcher.behavior(Map.empty), "root")
    //    val hello = Hello.deploy(actorSystem)(actorSystem.scheduler)
    //    val helloStateful = HelloForgetful.deploy(actorSystem)(actorSystem.scheduler)
    val helloEventSourced = HelloEventSourced.deploy(actorSystem)(actorSystem.scheduler)
    val helloDurableState = HelloDurableState.deploy(actorSystem)(actorSystem.scheduler)

    //    hello.map { hl =>
    //      hl.die()
    //      hl.greet("from the dead")
    //    }

    helloEventSourced.foreach { hl =>
      hl.greet("Foo")
    }

    helloDurableState.foreach { hl =>
      hl.greet("Foo")
    }



//    helloStateful.map { hl =>
//      hl.greet("Foo")
//        .flatMap(_ => hl.greet("Bar"))
//        .flatMap(_ => hl.greet("Baz"))
//        .flatMap(_ => {
//          hl.die()
//          Future.successful(Done.done())
//        })
//        .flatMap(_ => hl.greet("from the dead"))
//    }
//
//    helloEventSourced.map { hl =>
//      hl.greet("Foo")
//        .flatMap(_ => hl.greet("Bar"))
//        .flatMap(_ => hl.greet("Baz"))
//        .flatMap(_ => {
//          hl.die()
//          Future.successful(Done.done())
//        })
//        .flatMap(_ => hl.greet("from the dead"))
//    }
