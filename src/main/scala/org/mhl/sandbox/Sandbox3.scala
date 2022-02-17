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

object Sandbox3 {
  def main(args: Array[String]): Unit = {
    val numbers: Seq[Int] = Seq.range(1, 20)
    implicit val random: Random = Random.javaRandomToRandom(Random.self)
    val config = ConfigFactory.load()
    val actorSystem: ActorSystem[SandboxActor.Protocol] = ActorSystem[SandboxActor.Protocol](
      Behaviors.empty,
      name = "root",
      config = config.getConfig("third").withFallback(config.withoutPath("first").withoutPath("second"))
    )

    actorSystem.scheduler.scheduleAtFixedRate(0 seconds, 10 seconds) { () =>
      Computation.getAll(actorSystem)
        .map { computeInstances =>
          println(computeInstances)
          computeInstances.foreach { instance =>
            val startTime = DateTime.now()
            val n = Rand.choose(numbers)
            val future = instance.compute(n)
            future.foreach { result =>
              val duration = (startTime to DateTime.now()).toPeriod(PeriodType.millis()).getMillis
              println(s"Computation took $duration ms")
              println(s"Computation result $n! = $result")
            }
          }
        }
    }
  }
}
