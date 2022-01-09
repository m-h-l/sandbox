ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.6"

lazy val root = (project in file("."))
  .settings(
    name := "sandbox"
  )

val akkaVer = "2.6.18"

libraryDependencies ++= Seq(
  ("com.typesafe.akka" %% "akka-actor" % akkaVer),
  ("com.typesafe.akka" %% "akka-actor-typed" % akkaVer),
  ("com.typesafe.akka" %% "akka-persistence-typed" % akkaVer),
  ("com.typesafe.akka" %% "akka-persistence-testkit" % akkaVer % Test),
  ("com.typesafe.akka" %% "akka-serialization-jackson" % akkaVer),
  ("com.typesafe.akka" %% "akka-cluster-typed" % akkaVer),
  ("ch.qos.logback" % "logback-classic" % "1.2.7"),
  ("org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"),
  ("org.iq80.leveldb" % "leveldb" % "0.12"),
  ("com.github.nscala-time" %% "nscala-time" % "2.30.0")
)

