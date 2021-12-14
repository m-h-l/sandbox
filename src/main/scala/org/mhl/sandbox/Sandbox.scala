package org.mhl.sandbox

import akka.actor.typed.ActorSystem

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

object Rand {
  def choose[A](seq: Seq[A])(implicit random: Random): A =
    seq(random.nextInt(seq.length))
}

object Sandbox {
  def main(args: Array[String]): Unit = {

    val actorSystem: ActorSystem[Dispatcher.Protocol] = ActorSystem[Dispatcher.Protocol](Dispatcher.behavior(Map.empty), "root")
    val hello = Hello.deploy(actorSystem)(actorSystem.scheduler)
    val helloEventSourced = HelloEventSourced.deploy(actorSystem)(actorSystem.scheduler)
    //val helloDurableState = HelloDurableState.deploy(actorSystem)(actorSystem.scheduler)

    hello.map { hl =>
      hl.die()
      hl.greet("from the dead")
    }

    actorSystem.scheduler.scheduleAtFixedRate(5 seconds, 5 seconds) {
      new Runnable {
        val names = Seq("Foo", "Bar", "Baz", "Hurr", "Durr", "Herp", "Derp", "Boom")

        override def run(): Unit = {
          implicit val random: Random = Random.javaRandomToRandom(Random.self)
          val name = Rand.choose(names)
          if (name == "Boom") {
            helloEventSourced.foreach(_.die())
          } else {
            helloEventSourced.foreach(_.greet(name))
          }
        }
      }
    }


  }
}


