ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.0.2"

libraryDependencies ++= Seq(
  ("com.typesafe.akka" %% "akka-actor" % "2.6.17").cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-actor-typed" % "2.6.17").cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-persistence-typed" % "2.6.17").cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-persistence-testkit" % "2.6.17" % Test).cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-serialization-jackson" % "2.6.17").cross(CrossVersion.for3Use2_13)
)

lazy val root = (project in file("."))
  .settings(
    name := "sandbox"
  )
