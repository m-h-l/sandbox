package org.mhl.sandbox

import akka.actor.typed.ActorRef

trait PingPong
case class Ping(replyTo: ActorRef[PingPong]) extends PingPong
case class Pong() extends PingPong
