ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  ("com.typesafe.akka" %% "akka-actor" % "2.6.17"),
  ("com.typesafe.akka" %% "akka-actor-typed" % "2.6.17"),
  ("com.typesafe.akka" %% "akka-persistence-typed" % "2.6.17"),
  ("com.typesafe.akka" %% "akka-persistence-testkit" % "2.6.17" % Test),
  ("com.typesafe.akka" %% "akka-serialization-jackson" % "2.6.17"),
  ("ch.qos.logback" % "logback-classic" % "1.2.7"),
  ("org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"),
  ("org.iq80.leveldb" % "leveldb" % "0.12")
)

lazy val root = (project in file("."))
  .settings(
    name := "sandbox"
  )
