package org.mhl.sandbox

class SandboxActor(val name: String) {

  def say(text: String): Unit =
    println(s"$name says: $text")

}
