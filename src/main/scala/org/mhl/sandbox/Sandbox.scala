package org.mhl.sandbox

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random


object Rand {
  def choose[A](seq: Seq[A])(implicit random: Random): A =
    seq(random.nextInt(seq.length))
}

object Sandbox {

  val names = Seq("Foo", "Bar", "Baz", "Hurr", "Durr", "Herp", "Derp", "Boom")
  var n = 1

  def main(args: Array[String]): Unit = {

    implicit val random: Random = Random.javaRandomToRandom(Random.self)

    implicit val actorSystem: ActorSystem[Dispatcher.Protocol] = ActorSystem[Dispatcher.Protocol](
      Behaviors.withStash[Dispatcher.Protocol](20) { stash =>
        Behaviors.setup {
          context =>
            Hello.deploy(context)(context.system.scheduler)
            HelloForgetful.deploy(context)(context.system.scheduler)
            HelloEventSourced.deploy(context)(context.system.scheduler)
            Computation.deploy(context)(context.system.scheduler)
            stash.unstashAll(Dispatcher.behavior(Map.empty))
        }
      },
      name = "root"
    )

    actorSystem.scheduler.scheduleAtFixedRate(0 seconds, 8 seconds) { () =>
      val factorial = Computation.residingOn(actorSystem)
      factorial
        .map(_.compute(n))
        .foreach(result => {
          result.foreach(r => println(s"$n! = $r"))
        })
      n = n + 1
    }

    actorSystem.scheduler.scheduleAtFixedRate(0 seconds, 8 seconds) { () =>
      val hello = HelloForgetful.residingOn(actorSystem)
      hello.failed.foreach(f => f.printStackTrace())
      val name = Rand.choose(names)
      println(s"$name")
      if (name == "Boom") {
        hello.foreach(_.die())
      } else {
        hello.foreach(_.greet(name))
      }
    }
  }
}


