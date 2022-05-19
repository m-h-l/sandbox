package org.mhl.sandbox

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.github.nscala_time.time.Imports.{DateTime, richReadableInstant}
import com.typesafe.config.ConfigFactory
import org.joda.time.PeriodType

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
  val numbers: Seq[Int] = Seq.range(1, 10)

  def main(args: Array[String]): Unit = {

    implicit val random: Random = Random.javaRandomToRandom(Random.self)
    val config = ConfigFactory.load()
    val actorSystem: ActorSystem[SandboxActor.Protocol] = ActorSystem[SandboxActor.Protocol](
        Behaviors.setup {
          context =>
            Hello.deploy(context)
            HelloForgetful.deploy(context)
            HelloEventSourced.deploy(context)
            Computation.deploy(context)
            //SandboxRouter(10, Computation).deploy(context)
            Behaviors.empty
        },
      name = "root",
      config = config.getConfig("first").withFallback(config.withoutPath("first").withoutPath("second"))
    )


//    actorSystem.scheduler.scheduleAtFixedRate(0 seconds, 2 seconds) { () =>
//      Computation.getSingle(actorSystem)
//        .map { c =>
//          val startTime = DateTime.now()
//          val n = Rand.choose(numbers)
//          val future = c.compute(n)
//          (startTime, n, future)
//        }
//        .foreach(Function.tupled { (startTime, n, result) =>
//          result.foreach { result =>
//            val duration = (startTime to DateTime.now()).toPeriod(PeriodType.millis()).getMillis
//            println(s"Computation took $duration ms")
//            println(s"Computation result $n! = $result")
//          }
//        })
//    }

//    actorSystem.scheduler.scheduleAtFixedRate(0 seconds, 8 seconds) { () =>
//      val hello = HelloForgetful.getSingle(actorSystem)
//      hello.failed.foreach(f => f.printStackTrace())
//      val name = Rand.choose(names)
//      println(s"$name")
//      if (name == "Boom") {
//        hello.foreach(_.die())
//      } else {
//        hello.foreach(_.greet(name))
//      }
//    }
  }
}


